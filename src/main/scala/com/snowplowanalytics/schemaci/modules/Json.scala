package com.snowplowanalytics.schemaci.modules

import cats.implicits._
import com.snowplowanalytics.iglu.client.{CirceValidator, Client, ClientError, Resolver}
import com.snowplowanalytics.iglu.client.resolver.registries.Registry.{parse => _, _}
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.implicits._
import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import io.circe.{Json => CJson}
import io.circe.generic.auto._
import io.circe.parser._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.io.{BufferedSource, Source}

object Json {

  trait Service {
    def extractSchemaDependenciesFromManifest(path: String): IO[CliError, List[Schema.Key]]
  }

  // accessors
  def extractSchemaDependenciesFromManifest(path: String): ZIO[Json, CliError, List[Schema.Key]] =
    ZIO.accessM(_.get[Json.Service].extractSchemaDependenciesFromManifest(path))

  // implementations
  final class CirceImpl extends Service {

    override def extractSchemaDependenciesFromManifest(path: String): IO[CliError, List[Schema.Key]] = {
      val mapValidationError: ClientError => CliError =
        c => ParsingError("Manifest validation failed", new Exception(c.getMessage).some)
      val mapResolutionError: Throwable => CliError =
        t => ParsingError("Manifest schema resolution failed", t.some)

      val schemaKeys: CJson => IO[CliError, List[Schema.Key]] = data =>
        ZIO
          .fromEither(data.hcursor.get[List[Schema.Key]]("schemas"))
          .mapError(CliError.Json.ParsingError("Cannot extract 'schemas' from manifest", _))

      for {
        jsonString <- readFileToString(path)
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

    private def readFileToString(path: String): IO[CliError, String] = {
      val openFile: String => Task[BufferedSource] = path => Task.effect(Source.fromFile(path, "UTF-8"))
      val readFile: BufferedSource => Task[String] = file => Task.effect(file.getLines.mkString)
      val closeFile: BufferedSource => UIO[Unit]   = bs => Task.effect(bs.close()).ignore

      ZIO
        .bracket(openFile(path))(closeFile)(readFile)
        .mapError(CliError.GenericError(s"Cannot open/read $path", _))
    }

  }

  // layers
  def circeLayer: ULayer[Json] = ZLayer.succeed(new CirceImpl)
}
