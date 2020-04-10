package com.snowplowanalytics.schemaci.modules

import io.circe._
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._
import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio.SttpClient
import zio._

object JwtProvider {
  case class JwtRequest(
      clientId: String,
      clientSecret: String,
      audience: String,
      grantType: String,
      username: String,
      password: String
  )

  implicit val customConfig: Configuration       = Configuration.default.withSnakeCaseMemberNames
  implicit val snakyEncoder: Encoder[JwtRequest] = deriveConfiguredEncoder

  def getAccessToken(
      authServerBaseUrl: String,
      clientId: String,
      clientSecret: String,
      audience: String,
      username: String,
      password: String
  ): RIO[SttpClient, String] = {
    val request = basicRequest
      .body(JwtRequest(clientId, clientSecret, audience, "password", username, password))
      .post(uri"${authServerBaseUrl}/oauth/token")
      .response(asJsonAlways[Json])

    for {
      response <- SttpClient.send(request)
      json     <- Task.fromEither(response.body)
      token    <- Task.fromEither(json.hcursor.get[String]("access_token"))
    } yield token
  }
}
