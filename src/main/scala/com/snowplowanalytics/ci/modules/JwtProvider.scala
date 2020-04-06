package com.snowplowanalytics.ci.modules

import io.circe._
import io.circe.literal._
import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio.SttpClient
import zio._

object JwtProvider {
  def getAccessToken(
      authServerBaseUrl: String,
      clientId: String,
      clientSecret: String,
      audience: String,
      username: String,
      password: String
  ): RIO[SttpClient, String] = {
    val request = basicRequest
      .body(
        json"""
          {
            "client_id": ${clientId},
            "client_secret": ${clientSecret},
            "audience": ${audience},
            "grant_type": "password",
            "username": ${username},
            "password": ${password}
          }
          """
      )
      .post(uri"${authServerBaseUrl}/oauth/token")
      .response(asJsonAlways[Json])

    for {
      response <- SttpClient.send(request)
      json     <- Task.fromEither(response.body)
      token    <- Task.fromEither(json.hcursor.get[String]("access_token"))
    } yield token
  }
}
