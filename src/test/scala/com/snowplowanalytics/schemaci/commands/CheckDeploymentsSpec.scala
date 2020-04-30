package com.snowplowanalytics.schemaci.commands

import cats.effect.ExitCode
import com.snowplowanalytics.schemaci.JsonMock._
import com.snowplowanalytics.schemaci.JwtMock._
import com.snowplowanalytics.schemaci.SchemaApiMock._
import com.snowplowanalytics.schemaci.{URL, UUID}
import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.modules.{Json, Jwt, SchemaApi}
import eu.timepit.refined._
import zio.test._
import zio.test.environment.TestEnvironment
import zio.test.Assertion._
import zio.ULayer
import zio.test.mock.Expectation._

import scala.io.Source

object CheckDeploymentsSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("CheckDeployments spec")(
      testM(
        "should parse manifest, get a JWT token, invoke Schema API and " +
          "confirm that all schemas are deployed to the target env"
      ) {
        val username: String       = "username"
        val password: String       = "password"
        val environment: String    = "DEV"
        val apiBaseUrl: URL        = refineMV("https://example.com/api")
        val authServerBaseUrl: URL = refineMV("https://auth.com")
        val clientId: String       = "clientId"
        val clientSecret: String   = "clientSecret"
        val audience: URL          = refineMV("https://example.com/api")

        val command = CheckDeployments(
          "/",
          username,
          password,
          environment,
          apiBaseUrl,
          authServerBaseUrl,
          clientId,
          clientSecret,
          audience
        )

        val schemas: List[Schema.Key] = List(Schema.Key("com.vendor", "name", "jsonschema", "1-0-0"))
        val token: String             = "token"
        val orgId: UUID               = refineMV("18d4080d-b1c7-4d33-979d-3fd4be734cfb")

        val mockEnv: ULayer[Json with Jwt with SchemaApi] =
          (ExtractSchemaDependenciesFromManifest(
            isSubtype[Source](anything)
          ) returns value(schemas)) andThen
            (GetAccessToken(
              equalTo((authServerBaseUrl, clientId, clientSecret, audience, username, password))
            ) returns value(token)) andThen
            (ExtractOrganizationIdFromToken(
              equalTo((authServerBaseUrl, token))
            ) returns value(orgId)) andThen
            (CheckSchemaDeployment(
              equalTo((apiBaseUrl, token, orgId, environment, schemas.head))
            ) returns value(true))

        assertM(command.process.provideCustomLayer(mockEnv))(equalTo(ExitCode.Success))
      }
    )

}
