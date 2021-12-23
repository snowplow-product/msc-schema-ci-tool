package com.snowplowanalytics.datastructures.ci.modules

import cats.implicits._
import io.circe.{Json => CJson}
import sttp.client3._
import zio._

import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.errors.CliError.Auth.InvalidApiKey
import com.snowplowanalytics.datastructures.ci.errors.CliError.Json.ParsingError
import com.snowplowanalytics.datastructures.ci.modules.Http.SttpRequest
import com.snowplowanalytics.datastructures.ci.{URL, UUID}

object Jwt {

  trait Service {

    def getAccessToken(apiBaseUrl: URL, organizationId: UUID, apiKey: String): IO[CliError, String]

  }

  // accessors
  def getAccessToken(apiBaseUrl: URL, organizationId: UUID, apiKey: String): ZIO[Jwt, CliError, String] =
    ZIO.accessM(_.get[Jwt.Service].getAccessToken(apiBaseUrl, organizationId, apiKey))

  // implementations
  final class Auth0Impl(http: Http.Service) extends Service {

    override def getAccessToken(apiBaseUrl: URL, organizationId: UUID, apiKey: String): IO[CliError, String] = {
      val request: SttpRequest =
        basicRequest
          .get(uri"$apiBaseUrl/organizations/$organizationId/credentials/v2/token")
          .header("X-API-Key", apiKey)

      val accessTokenExtractor: CJson => Either[ParsingError, String] =
        _.hcursor
          .get[String]("accessToken")
          .leftMap(CliError.Json.ParsingError("Cannot extract 'access token' from response", _))

      http
        .sendRequest(request, accessTokenExtractor)
        .catchSome {
          case _: ParsingError => IO.fail(InvalidApiKey)
        }
    }

  }

  // layers
  def auth0Layer: URLayer[Http, Jwt] = ZLayer.fromService(new Auth0Impl(_))
}
