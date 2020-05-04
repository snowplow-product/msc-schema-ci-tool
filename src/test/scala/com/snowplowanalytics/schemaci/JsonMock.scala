package com.snowplowanalytics.schemaci

import scala.io.Source

import zio.test.mock.{Method, Proxy}
import zio.{Has, IO, URLayer, ZLayer}

import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.modules.Json

object JsonMock {

  sealed trait Tag[I, A] extends Method[Json, I, A] {
    def envBuilder = JsonMock.envBuilder
  }

  object ExtractSchemaDependenciesFromManifest extends Tag[Source, List[Schema.Key]]

  private lazy val envBuilder: URLayer[Has[Proxy], Json] =
    ZLayer.fromService(invoke =>
      new Json.Service {

        def extractSchemaDependenciesFromManifest(source: => Source): IO[CliError, List[Schema.Key]] =
          // ignore original source in mock to avoid eager evaluation
          invoke(ExtractSchemaDependenciesFromManifest, Source.fromString(""))

      }
    )

}
