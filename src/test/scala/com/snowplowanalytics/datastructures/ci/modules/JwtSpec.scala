package com.snowplowanalytics.datastructures.ci.modules

import eu.timepit.refined.auto._
import io.circe.literal._
import io.circe.{Json => CJson}
import sttp.client3.{Request, Response}
import sttp.model.Header
import zio.ULayer
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import com.snowplowanalytics.datastructures.ci.TestFixtures._
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.Jwt._
import com.snowplowanalytics.datastructures.ci.{URL, UUID}

object JwtSpec extends DefaultRunnableSpec {

  private val apiBaseUrl: URL      = "https://example.com"
  private val apiKey: String       = "key"
  private val organizationId: UUID = "18d4080d-b1c7-4d33-979d-3fd4be734cfb"

  object GetAccessTokenFixtures {

    private[JwtSpec] val matchRequest: Request[_, _] => Boolean =
      r =>
        r.uri.toString == s"$apiBaseUrl/organizations/$organizationId/credentials/v2/token" &&
          r.headers.contains(Header("X-API-Key", apiKey))

    private[JwtSpec] val ok: Response[CJson] =
      Response.ok(
        json"""
         {
           "accessToken": "token"
         }
         """
      )

    private[JwtSpec] val serverStub: ULayer[Http] =
      httpLayerFromSttpStub(sttpBackendStubForGet(matchRequest, ok))

  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Jwt spec")(
      suite("getAccessToken")(
        testM("should fail if api key is invalid") {
          assertM(
            getAccessToken(apiBaseUrl, organizationId, "invalid api key").run
          )(
            fails(equalTo(CliError.Auth.InvalidApiKey))
          )
        },
        testM("should extract `accessToken` from the auth server's response when request succeeds") {
          assertM(
            getAccessToken(apiBaseUrl, organizationId, apiKey)
          )(
            equalTo("token")
          )
        }
      ).provideCustomLayer(GetAccessTokenFixtures.serverStub >>> Jwt.auth0Layer)
    )

}
