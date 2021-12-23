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
        "should parse manifest, get a JWT token, invoke Data Structures API and " +
          "confirm that all schemas are deployed to the target env"
      ) {
        val environment: String = "DEV"
        val apiUrl: URL         = "https://example.com/api"
        val orgId: UUID         = "3e31e38c-001c-4edf-99bb-d2a9c2925bcb"
        val apiKey: String      = "key"

        val command = CheckDeployments(
          "/",
          environment,
          apiUrl,
          orgId,
          apiKey
        )

        val schemas: List[Key] = List(Schema.Key("com.vendor", "name", "jsonschema", "1-0-0"))
        val token: String      = "token"

        val mockEnv: ULayer[Json with Jwt with DataStructuresApi] =
          ExtractSchemaDependenciesFromManifest(isSubtype[Source](anything), value(schemas)) ++
            GetAccessToken(equalTo((apiUrl, orgId, apiKey)), value(token)) ++
            CheckSchemaDeployment(equalTo((apiUrl, token, orgId, environment, schemas.head)), value(true))

        assertM(command.process.provideCustomLayer(mockEnv))(equalTo(ExitCode.Success))
      }
    )

}
