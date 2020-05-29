package com.snowplowanalytics.datastructures.ci

import zio.test.mock.{Method, Proxy}
import zio.{Has, IO, URLayer, ZLayer}

import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.Jwt

object JwtMock {

  sealed trait Tag[I, A] extends Method[Jwt, I, A] {
    def envBuilder = JwtMock.envBuilder
  }

  object GetAccessToken                 extends Tag[(URL, String, String, URL, String, String), String]
  object ExtractOrganizationIdFromToken extends Tag[(URL, String), UUID]

  private lazy val envBuilder: URLayer[Has[Proxy], Jwt] =
    ZLayer.fromService(invoke =>
      new Jwt.Service {

        override def getAccessToken(
          authServerBaseUrl: URL,
          clientId: String,
          clientSecret: String,
          audience: URL,
          username: String,
          password: String
        ): IO[CliError, String] =
          invoke(GetAccessToken, authServerBaseUrl, clientId, clientSecret, audience, username, password)

        override def extractOrganizationIdFromToken(authServerBaseUrl: URL, token: String): IO[CliError, UUID] =
          invoke(ExtractOrganizationIdFromToken, authServerBaseUrl, token)

      }
    )

}
