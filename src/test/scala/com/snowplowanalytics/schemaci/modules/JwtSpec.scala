package com.snowplowanalytics.schemaci.modules

import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.modules.Jwt._
import com.snowplowanalytics.schemaci.URL
import com.snowplowanalytics.schemaci.entities.JwtRequest
import com.snowplowanalytics.schemaci.modules.TestFixtures._
import eu.timepit.refined._
import io.circe.{ Json => CJson }
import io.circe.literal._
import sttp.client.Response
import sttp.model.StatusCode._
import sttp.model.Uri
import zio.test._
import zio.test.Assertion._
import zio.test.environment.TestEnvironment

object JwtSpec extends DefaultRunnableSpec {

  private val authServerUrl: URL   = refineMV("https://example.com")
  private val audience: URL        = refineMV("https://example.com")
  private val clientId: String     = "cid"
  private val clientSecret: String = "cs"
  private val user: String         = "u"
  private val password: String     = "p"

  private val matchUri: Uri => Boolean =
    _.toString.startsWith(authServerUrl.value)

  private val answer: JwtRequest => Response[CJson] = {
    case JwtRequest("cid", "cs", "https://example.com", "password", "u", "p") => ok
    case JwtRequest("cid", "cs", _, "password", "u", "p")                     => Response(json"{}", BadRequest)
    case JwtRequest("cid", "cs", _, "password", _, _)                         => Response(json"{}", Forbidden)
    case JwtRequest(_, _, _, "password", _, _)                                => Response(json"{}", Unauthorized)
    case JwtRequest(_, _, _, _, _, _)                                         => Response(json"{}", Forbidden)
  }

  private val ok: Response[CJson] =
    Response.ok(
      json"""
         {
           "access_token": "token",
           "expires_in": 86400,
           "token_type": "Bearer"
         }
         """
    )

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Jwt spec")(
      suite("getAccessToken")(
        testM("should fail if url is wrong")(
          assertM(
            getAccessToken(refineMV("https://wrong.com"), clientId, clientSecret, audience, user, password).run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        ),
        testM("should fail if clientId/clientSecret are invalid")(
          assertM(
            getAccessToken(authServerUrl, "wrong", "wrong", audience, user, password).run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        ),
        testM("should fail if user/password are invalid")(
          assertM(
            getAccessToken(authServerUrl, clientId, clientSecret, audience, "wrong", "wrong").run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        ),
        testM("should fail if audience is invalid")(
          assertM(
            getAccessToken(authServerUrl, clientId, clientSecret, refineMV("https://wrong.com"), user, password).run
          )(
            fails(equalTo(CliError.Auth.InvalidCredentials))
          )
        ),
        testM("should extract `access_token` from the auth server's response when request succeeds")(
          assertM(
            getAccessToken(authServerUrl, clientId, clientSecret, audience, user, password)
          )(
            equalTo("token")
          )
        )
      )
    ).provideCustomLayer(httpLayerFromSttpStub(sttpBackendStub(matchUri, answer)) >>> Jwt.auth0Layer)

}
