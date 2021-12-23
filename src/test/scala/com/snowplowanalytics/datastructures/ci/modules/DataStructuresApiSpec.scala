package com.snowplowanalytics.datastructures.ci.modules

import eu.timepit.refined.auto._
import io.circe.literal._
import io.circe.{Json => CJson}
import sttp.client3.{Request, Response}
import sttp.model.Header
import sttp.model.Uri.QuerySegment
import zio.ULayer
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import com.snowplowanalytics.datastructures.ci.TestFixtures._
import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.modules.DataStructuresApi.{checkSchemaDeployment, liveLayer}
import com.snowplowanalytics.datastructures.ci.{URL, UUID}

object DataStructuresApiSpec extends DefaultRunnableSpec {

  private val apiBaseUrl: URL       = "https://example.com"
  private val token: String         = "token"
  private val organizationId: UUID  = "18d4080d-b1c7-4d33-979d-3fd4be734cfb"
  private val schemaKey: Schema.Key = Schema.Key("com.vendor", "name", "jsonschema", "1-0-0")
  private val hash: String          = "4d17d63e1acdb882757470f2be83a30e1dfc82c2e09519ed5fa5cc5e788bd3e2"

  private def matchRequest(env: String): Request[_, _] => Boolean = { r =>
    r.uri.toString.startsWith(s"$apiBaseUrl/organizations/$organizationId/data-structures/v1/$hash/deployments") &&
    r.uri.querySegments.contains(QuerySegment.KeyValue("env", env)) &&
    r.uri.querySegments.contains(QuerySegment.KeyValue("version", schemaKey.version)) &&
    r.headers.contains(Header.unsafeApply("Authorization", "Bearer token"))
  }

  private val found: CJson    = json"""[{ "version": "1-0-0", "env": "DEV" }]"""
  private val notFound: CJson = json"""[]"""

  private def backendStub(env: String, response: CJson): ULayer[Http] =
    httpLayerFromSttpStub(sttpBackendStubForGet(matchRequest(env), Response.ok(response)))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("DataStructuresApi spec")(
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
