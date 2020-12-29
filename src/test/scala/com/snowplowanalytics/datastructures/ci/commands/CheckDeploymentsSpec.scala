package com.snowplowanalytics.datastructures.ci.commands

import scala.io.Source

import cats.effect.ExitCode
import eu.timepit.refined.auto._
import zio.ULayer
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.mock.Expectation._

import com.snowplowanalytics.datastructures.ci.DataStructuresApiMock._
import com.snowplowanalytics.datastructures.ci.JsonMock._
import com.snowplowanalytics.datastructures.ci.JwtMock._
import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.entities.Schema.Key
import com.snowplowanalytics.datastructures.ci.modules.{DataStructuresApi, Json, Jwt}
import com.snowplowanalytics.datastructures.ci.{URL, UUID}

object CheckDeploymentsSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CheckDeployments spec")(
      testM(
        "should parse manifest, get a JWT token, invoke Schema API and " +
          "confirm that all schemas are deployed to the target env"
      ) {
        val username: String     = "username"
        val password: String     = "password"
        val environment: String  = "DEV"
        val apiUrl: URL          = "https://example.com/api"
        val authServerUrl: URL   = "https://auth.com"
        val clientId: String     = "clientId"
        val clientSecret: String = "clientSecret"
        val aud: URL             = "https://example.com/api"

        val command = CheckDeployments(
          "/",
          username,
          password,
          environment,
          apiUrl,
          authServerUrl,
          clientId,
          clientSecret,
          aud
        )

        val schemas: List[Key] = List(Schema.Key("com.vendor", "name", "jsonschema", "1-0-0"))
        val token: String      = "token"
        val orgId: UUID        = "18d4080d-b1c7-4d33-979d-3fd4be734cfb"

        val mockEnv: ULayer[Json with Jwt with DataStructuresApi] =
          ExtractSchemaDependenciesFromManifest(isSubtype[Source](anything), value(schemas)) ++
            GetAccessToken(equalTo((authServerUrl, clientId, clientSecret, aud, username, password)), value(token)) ++
            ExtractOrganizationIdFromToken(equalTo((authServerUrl, token)), value(orgId)) ++
            CheckSchemaDeployment(equalTo((apiUrl, token, orgId, environment, schemas.head)), value(true))

        assertM(command.process.provideCustomLayer(mockEnv))(equalTo(ExitCode.Success))
      }
    )

}
