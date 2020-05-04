package com.snowplowanalytics.schemaci.modules

import com.snowplowanalytics.schemaci.{URL, UUID}
import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.modules.SchemaApi.{checkSchemaDeployment, liveLayer}
import com.snowplowanalytics.schemaci.TestFixtures._
import eu.timepit.refined.refineMV
import io.circe.{Json => CJson}
import io.circe.literal._
import sttp.client.{Request, Response}
import sttp.model.Header
import sttp.model.Uri.QuerySegment
import zio.test._
import zio.ULayer
import zio.test.Assertion._
import zio.test.environment.TestEnvironment

object SchemaApiSpec extends DefaultRunnableSpec {

  private val apiBaseUrl: URL       = refineMV("https://example.com")
  private val token: String         = "token"
  private val organizationId: UUID  = refineMV("18d4080d-b1c7-4d33-979d-3fd4be734cfb")
  private val schemaKey: Schema.Key = Schema.Key("com.vendor", "name", "jsonschema", "1-0-0")
  private val hash: String          = "4d17d63e1acdb882757470f2be83a30e1dfc82c2e09519ed5fa5cc5e788bd3e2"

  private def matchRequest(env: String): Request[_, _] => Boolean = { r =>
    r.uri.toString.startsWith(s"$apiBaseUrl/api/schemas/v1/organizations/$organizationId/schemas/$hash/deployments") &&
    r.uri.querySegments.contains(QuerySegment.KeyValue("env", env)) &&
    r.uri.querySegments.contains(QuerySegment.KeyValue("version", schemaKey.version)) &&
    r.headers.contains(Header.unsafeApply("Authorization", "Bearer token"))
  }

  private val found: CJson    = json"""[{ "version": "1-0-0", "env": "DEV" }]"""
  private val notFound: CJson = json"""[]"""

  private def backendStub(env: String, response: CJson): ULayer[Http] =
    httpLayerFromSttpStub(sttpBackendStubForGet(matchRequest(env), Response.ok(response)))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("SchemaApi spec")(
      suite("checkSchemaDeployment")(
        testM("should return true if the given schema is deployed to the specified environment") {
          assertM(
            checkSchemaDeployment(apiBaseUrl, token, organizationId, "DEV", schemaKey)
              .provideCustomLayer(backendStub("DEV", found) >>> liveLayer)
          )(
            equalTo(true)
          )
        },
        testM("should return false if the given schema is not deployed to the specified environment") {
          assertM(
            checkSchemaDeployment(apiBaseUrl, token, organizationId, "PROD", schemaKey)
              .provideCustomLayer(backendStub("PROD", notFound) >>> liveLayer)
          )(
            equalTo(false)
          )
        }
      )
    )

}
