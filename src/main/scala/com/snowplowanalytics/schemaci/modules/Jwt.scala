package com.snowplowanalytics.schemaci.modules

import java.security.PublicKey

import cats.implicits._
import com.chatwork.scala.jwk.{JWK, JWKSet, KeyId, RSAJWK}
import com.chatwork.scala.jwk.JWSAlgorithmType.AlgorithmFamily._
import com.snowplowanalytics.schemaci.{URL, UUID}
import com.snowplowanalytics.schemaci.entities.JwtRequest
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Auth.InvalidCredentials
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import com.snowplowanalytics.schemaci.modules.Http.SttpRequest
import eu.timepit.refined.refineV
import eu.timepit.refined.string.Uuid
import io.circe.{Json => CJson}
import io.circe.parser._
import pdi.jwt.{JwtCirce, JwtHeader, JwtOptions}
import sttp.client._
import sttp.client.circe._
import zio._

object Jwt {

  trait Service {

    def getAccessToken(
      authServerBaseUrl: URL,
      clientId: String,
      clientSecret: String,
      audience: URL,
      username: String,
      password: String
    ): IO[CliError, String]

    def extractOrganizationIdFromToken(authServerBaseUrl: URL, token: String): IO[CliError, UUID]
  }

  // accessors
  def getAccessToken(
    authServerBaseUrl: URL,
    clientId: String,
    clientSecret: String,
    audience: URL,
    username: String,
    password: String
  ): ZIO[Jwt, CliError, String] =
    ZIO.accessM(
      _.get[Jwt.Service].getAccessToken(authServerBaseUrl, clientId, clientSecret, audience, username, password)
    )

  def extractOrganizationIdFromToken(authServerBaseUrl: URL, token: String): ZIO[Jwt, CliError, UUID] =
    ZIO.accessM(_.get[Jwt.Service].extractOrganizationIdFromToken(authServerBaseUrl, token))

  // implementations
  final class Auth0Impl(http: Http.Service) extends Service {

    override def getAccessToken(
      authServerBaseUrl: URL,
      clientId: String,
      clientSecret: String,
      audience: URL,
      username: String,
      password: String
    ): IO[CliError, String] = {
      val request: SttpRequest = basicRequest
        .body(JwtRequest(clientId, clientSecret, audience.value, "password", username, password))
        .post(uri"$authServerBaseUrl/oauth/token")

      val accessTokenExtractor: CJson => Either[ParsingError, String] =
        _.hcursor
          .get[String]("access_token")
          .leftMap(CliError.Json.ParsingError("Cannot extract 'access token' from response", _))

      http
        .sendRequest(request, accessTokenExtractor)
        .catchSome {
          case _: ParsingError => IO.fail(InvalidCredentials)
        }
    }

    override def extractOrganizationIdFromToken(authServerBaseUrl: URL, token: String): IO[CliError, UUID] = {
      val request: SttpRequest =
        basicRequest.get(uri"$authServerBaseUrl/.well-known/jwks.json")

      val extractJwkSet: CJson => Either[ParsingError, JWKSet] =
        _.hcursor
          .as[JWKSet]
          .leftMap(ParsingError("Unexpected response format", _))

      val getRsaPublicKey: (JwtHeader, JWKSet) => Option[PublicKey] = { (header, keys) =>
        val rsaToPublicKey: PartialFunction[JWK, Option[PublicKey]] = {
          case rsa: RSAJWK => rsa.toPublicKey.toOption
        }
        for {
          kid       <- header.keyId
          alg       <- header.algorithm
          jwk       <- keys.keyByKeyId(KeyId(kid))
          _         <- jwk.algorithmType.filter(algType => RSA.values.contains(algType) && algType.entryName == alg.name)
          rsaPubKey <- rsaToPublicKey.lift.andThen(_.flatten)(jwk)
        } yield rsaPubKey
      }

      val extractOrganizationId: String => Either[ParsingError, UUID] =
        parse(_)
          .flatMap(
            _.hcursor
              .downField("https://snowplowanalytics.com/roles")
              .downField("user")
              .downField("organization")
              .get[String]("id")
          )
          .flatMap(refineV[Uuid](_))
          .leftMap(_ => ParsingError("Cannot extract Organization Id as UUID"))

      (for {
        keys      <- http.sendRequest(request, extractJwkSet)
        header    <- ZIO.fromTry(JwtCirce.decodeAll(token, JwtOptions(signature = false)).map(_._1))
        rsaKey    <- ZIO.fromEither(getRsaPublicKey(header, keys).toRight(new Exception("Cannot retrieve JWK")))
        rawClaims <- ZIO.fromTry(JwtCirce.decodeRaw(token, rsaKey))
        orgId     <- ZIO.fromEither(extractOrganizationId(rawClaims))
      } yield orgId).mapError(t => CliError.Auth.InvalidToken(t.some))
    }

  }

  // layers
  def auth0Layer: URLayer[Http, Jwt] = ZLayer.fromService(new Auth0Impl(_))
}
