package com.snowplowanalytics.schemaci.modules

import java.security.PublicKey

import cats.implicits._
import com.chatwork.scala.jwk.{JWK, JWKSet, KeyId, RSAJWK}
import com.chatwork.scala.jwk.JWSAlgorithmType.AlgorithmFamily._
import com.snowplowanalytics.schemaci.entities.JwtRequest
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Auth.InvalidCredentials
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import com.snowplowanalytics.schemaci.{URL, UUID}
import eu.timepit.refined.refineV
import eu.timepit.refined.string.Uuid
import io.circe._
import io.circe.parser._
import pdi.jwt.{JwtCirce, JwtHeader, JwtOptions}
import sttp.client._
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe._
import zio._

object JwtProvider {
  def getAccessToken(
      authServerBaseUrl: URL,
      clientId: String,
      clientSecret: String,
      audience: URL,
      username: String,
      password: String
  ): ZIO[SttpClient, CliError, String] = {
    val request: Request[Either[String, String], Nothing] = basicRequest
      .body(JwtRequest(clientId, clientSecret, audience.value, "password", username, password))
      .post(uri"$authServerBaseUrl/oauth/token")

    val accessTokenExtractor: Json => Either[ParsingError, String] =
      _.hcursor
        .get[String]("access_token")
        .leftMap(CliError.Json.ParsingError("Cannot extract 'access token' from response", _))

    HttpClient
      .sendRequest(request, accessTokenExtractor)
      .catchSome {
        case _: ParsingError => IO.fail(InvalidCredentials)
      }
  }

  def extractOrganizationIdFromToken(authServerBaseUrl: URL, token: String): ZIO[SttpClient, CliError, UUID] = {
    val request: Request[Either[String, String], Nothing] =
      basicRequest.get(uri"$authServerBaseUrl/.well-known/jwks.json")

    val extractJwkSet: Json => Either[ParsingError, JWKSet] =
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
      keys      <- HttpClient.sendRequest(request, extractJwkSet)
      header    <- ZIO.fromTry(JwtCirce.decodeAll(token, JwtOptions(signature = false)).map(_._1))
      rsaKey    <- ZIO.fromEither(getRsaPublicKey(header, keys).toRight(new Exception("Cannot retrieve JWK")))
      rawClaims <- ZIO.fromTry(JwtCirce.decodeRaw(token, rsaKey))
      orgId     <- ZIO.fromEither(extractOrganizationId(rawClaims))
    } yield orgId).mapError(t => CliError.Auth.InvalidToken(t.some))
  }
}
