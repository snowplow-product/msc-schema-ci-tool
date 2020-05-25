package com.snowplowanalytics.datastructures.ci.modules

import scala.io.Source

import cats.implicits._
import io.circe.generic.auto._
import io.circe.parser._
import io.circe.{Json => CJson}
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import com.snowplowanalytics.iglu.client.resolver.registries.Registry.{parse => _, _}
import com.snowplowanalytics.iglu.client.{CirceValidator, Client, ClientError, Resolver}
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.implicits._
import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.errors.CliError.Json.ParsingError

object Json {

  trait Service {
    def extractSchemaDependenciesFromManifest(source: => Source): IO[CliError, List[Schema.Key]]
  }

  // accessors
  def extractSchemaDependenciesFromManifest(source: => Source): ZIO[Json, CliError, List[Schema.Key]] =
    ZIO.accessM(_.get[Json.Service].extractSchemaDependenciesFromManifest(source))

  // implementations
  final class CirceImpl extends Service {

    override def extractSchemaDependenciesFromManifest(source: => Source): IO[CliError, List[Schema.Key]] = {
      val mapValidationError: ClientError => CliError =
        c => ParsingError("Manifest validation failed", new Exception(c.getMessage).some)
      val mapResolutionError: Throwable => CliError =
        t => ParsingError("Manifest schema resolution failed", t.some)

      val schemaKeys: CJson => IO[CliError, List[Schema.Key]] = data =>
        ZIO
          .fromEither(data.hcursor.get[List[Schema.Key]]("schemas"))
          .mapError(CliError.Json.ParsingError("Cannot extract 'schemas' from manifest", _))

      for {
        jsonString <- readFileToString(source)
        json       <- parseJson(jsonString)
        manifest   <- parseSelfDescribingJsonData(json)
        client      = Client[Task, CJson](Resolver(List(EmbeddedRegistry), None), CirceValidator)
        _          <- client.check(manifest).leftMap(mapValidationError).value.mapError(mapResolutionError).absolve
        schemaKeys <- schemaKeys(manifest.data)
      } yield schemaKeys
    }

    private def parseSelfDescribingJsonData(selfDescJsonData: CJson): IO[CliError, SelfDescribingData[CJson]] =
      ZIO
        .fromEither(SelfDescribingData.parse(selfDescJsonData))
        .mapError(CliError.Json.ParsingError("Self Describing Data parsing failure", _))

    private def parseJson(jsonString: String): IO[CliError, CJson] =
      ZIO
        .fromEither(parse(jsonString))
        .mapError(CliError.Json.ParsingError("JSON parsing failure", _))

    private def readFileToString(source: => Source): IO[CliError, String] =
      ZIO
        .bracket(Task.effect(source))(s => Task.effect(s.close()).ignore)(s => Task.effect(s.getLines.mkString))
        .mapError(CliError.GenericError(s"Cannot open/read ${source.descr}", _))

  }

  // layers
  def circeLayer: ULayer[Json] = ZLayer.succeed(new CirceImpl)
}
