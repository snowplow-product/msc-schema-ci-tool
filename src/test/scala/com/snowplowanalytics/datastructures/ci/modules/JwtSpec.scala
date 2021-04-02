package com.snowplowanalytics.datastructures.ci.modules

import eu.timepit.refined.auto._
import io.circe.literal._
import io.circe.{Json => CJson}
import sttp.client.{Request, Response}
import sttp.model.StatusCode._
import zio.ULayer
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import com.snowplowanalytics.datastructures.ci.TestFixtures._
import com.snowplowanalytics.datastructures.ci.URL
import com.snowplowanalytics.datastructures.ci.entities.JwtRequest
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.Jwt._

object JwtSpec extends DefaultRunnableSpec {

  private val authServerUrl: URL   = "https://example.com"
  private val audience: URL        = "https://example.com"
  private val clientId: String     = "cid"
  private val clientSecret: String = "cs"
  private val user: String         = "u"
  private val password: String     = "p"

  object GetAccessTokenFixtures {

    private[JwtSpec] val matchRequest: Request[_, _] => Boolean =
      _.uri.toString == authServerUrl.value + "/oauth/token"

    private[JwtSpec] val answer: JwtRequest => Response[CJson] = {
      case JwtRequest("cid", "cs", "https://example.com", "password", "u", "p") => ok
      case JwtRequest("cid", "cs", _, "password", "u", "p")                     => Response(json"{}", BadRequest)
      case JwtRequest("cid", "cs", _, "password", _, _)                         => Response(json"{}", Forbidden)
      case JwtRequest(_, _, _, "password", _, _)                                => Response(json"{}", Unauthorized)
      case JwtRequest(_, _, _, _, _, _)                                         => Response(json"{}", Forbidden)
    }

    private[JwtSpec] val ok: Response[CJson] =
      Response.ok(
        json"""
         {
           "access_token": "token",
           "expires_in": 86400,
           "token_type": "Bearer"
         }
         """
      )

    private[JwtSpec] val serverStub: ULayer[Http] =
      httpLayerFromSttpStub(sttpBackendStubForPost(matchRequest, answer))

  }

  object ExtractOrganizationIdFromTokenFixtures {

    private[JwtSpec] val matchRequest: Request[_, _] => Boolean =
      _.uri.toString == authServerUrl.value + "/.well-known/jwks.json"

    private[JwtSpec] def serverStub(answer: Response[CJson]): ULayer[Http] =
      httpLayerFromSttpStub(sttpBackendStubForGet(matchRequest, answer))

  }

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Jwt spec")(
      suite("getAccessToken")(
        testM("should fail if url is wrong") {
          assertM(
            getAccessToken("https://wrong.com", clientId, clientSecret, audience, user, password).run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        },
        testM("should fail if clientId/clientSecret are invalid") {
          assertM(
            getAccessToken(authServerUrl, "wrong", "wrong", audience, user, password).run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        },
        testM("should fail if user/password are invalid") {
          assertM(
            getAccessToken(authServerUrl, clientId, clientSecret, audience, "wrong", "wrong").run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        },
        testM("should fail if audience is invalid") {
          assertM(
            getAccessToken(authServerUrl, clientId, clientSecret, "https://wrong.com", user, password).run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        },
        testM("should extract `access_token` from the auth server's response when request succeeds") {
          assertM(
            getAccessToken(authServerUrl, clientId, clientSecret, audience, user, password)
          )(
            equalTo("token")
          )
        }
      ).provideCustomLayer(GetAccessTokenFixtures.serverStub >>> Jwt.auth0Layer),
      suite("extractOrganizationIdFromToken")(
        testM("should fail if auth server's response is not deserializable as a JWK set") {
          val env = ExtractOrganizationIdFromTokenFixtures.serverStub(Response.ok(json"{}")) >>> Jwt.auth0Layer
          assertM(
            extractOrganizationIdFromToken(authServerUrl, "token").run.provideCustomLayer(env)
          )(
            fails(isSubtype[CliError.Auth](anything))
          )
        }
      )
    )

}
