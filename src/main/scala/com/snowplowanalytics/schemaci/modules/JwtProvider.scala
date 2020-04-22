package com.snowplowanalytics.schemaci.modules

import cats.syntax.either._
import com.snowplowanalytics.schemaci.entities.JwtRequest
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Auth.InvalidCredentials
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import sttp.client._
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe._
import zio._

object JwtProvider {
  implicit val customConfig: Configuration       = Configuration.default.withSnakeCaseMemberNames
  implicit val snakyEncoder: Encoder[JwtRequest] = deriveConfiguredEncoder

  def getAccessToken(
      authServerBaseUrl: String,
      clientId: String,
      clientSecret: String,
      audience: String,
      username: String,
      password: String
  ): ZIO[SttpClient, CliError, String] = {
    val request: Request[Either[String, String], Nothing] = basicRequest
      .body(JwtRequest(clientId, clientSecret, audience, "password", username, password))
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
}
