package com.snowplowanalytics.datastructures.ci.modules

import com.snowplowanalytics.datastructures.ci.entities.Schema

import scala.io.Source
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.Json.{circeLayer, extractSchemaDependenciesFromManifest}

object JsonSpec extends DefaultRunnableSpec {

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Json spec")(
      suite("extractSchemaDependenciesFromManifest")(
        testM("should fail if source cannot be read") {
          assertM(
            extractSchemaDependenciesFromManifest(Source.fromResource("bad/path")).run
          )(
            fails(isSubtype[CliError.GenericError](anything))
          )
        },
        testM("should fail if source is not parsable as JSON") {
          assertM(
            extractSchemaDependenciesFromManifest(Source.fromChars("{]".toCharArray)).run
          )(
            fails(isSubtype[CliError.Json.ParsingError](anything))
          )
        },
        testM("should fail if source is not parsable as Self Describing Data") {
          assertM(
            extractSchemaDependenciesFromManifest(Source.fromChars("""{ "schema": "", "data": {} }""".toCharArray)).run
          )(
            fails(isSubtype[CliError.Json.ParsingError](anything))
          )
        },
        testM("should fail if source does not conform to Self Describing JSON Schema") {
          assertM(
            extractSchemaDependenciesFromManifest(Source.fromResource("manifest/notConformingToSchema.json")).run
          )(
            fails(isSubtype[CliError.Json.ParsingError](anything))
          )
        },
        testM("should fail if source does not conform to Self Describing JSON Schema") {
          assertM(
            extractSchemaDependenciesFromManifest(Source.fromResource("manifest/invalidSchemaReference.json")).run
          )(
            fails(isSubtype[CliError.Json.ParsingError](anything))
          )
        },
        testM("should extract a list of schema dependencies from manifest") {
          assertM(
            extractSchemaDependenciesFromManifest(Source.fromResource("manifest/valid.json"))
          )(
            hasSameElements(
              List(
                Schema.Key("com.snplow.msc.aws", "another_example_event", "jsonschema", "1-0-0"),
                Schema.Key("com.snplow.msc.gcp", "example_event", "jsonschema", "1-0-7")
              )
            )
          )
        }
      )
    ).provideCustomLayer(circeLayer)

}
