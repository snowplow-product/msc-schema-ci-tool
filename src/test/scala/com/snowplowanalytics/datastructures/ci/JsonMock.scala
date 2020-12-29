package com.snowplowanalytics.datastructures.ci

import scala.io.Source

import zio.test.mock._
import zio.{Has, IO, URLayer, ZLayer}

import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.Json

object JsonMock extends Mock[Json] {

  object ExtractSchemaDependenciesFromManifest extends Effect[Source, CliError, List[Schema.Key]]

  val compose: URLayer[Has[Proxy], Json] =
    ZLayer.fromService(invoke =>
      new Json.Service {

        def extractSchemaDependenciesFromManifest(source: => Source): IO[CliError, List[Schema.Key]] =
          // ignore original source in mock to avoid eager evaluation
          invoke(ExtractSchemaDependenciesFromManifest, Source.fromString(""))

      }
    )

}
