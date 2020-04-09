package com.snowplowanalytics.schemaci.modules

import cats.implicits._
import com.snowplowanalytics.schemaci.modules.SchemaApiClient.Schema
import com.snowplowanalytics.iglu.client.{CirceValidator, Client, Resolver}
import com.snowplowanalytics.iglu.client.resolver.registries.Registry.{parse => _, _}
import com.snowplowanalytics.iglu.core.SelfDescribingData
import com.snowplowanalytics.iglu.core.circe.implicits._
import io.circe.Json
import io.circe.generic.auto._
import io.circe.parser._
import zio._
import zio.interop.catz._
import zio.interop.catz.implicits._

import scala.io.{BufferedSource, Source}

object JsonProvider {
  def extractUsedSchemasFromManifest(path: String): Task[List[Schema.Metadata]] =
    readFileToString(path) >>= parseJson >>= validateSelfDescribingJsonAndExtractData >>= getUsedSchemas

  private def parseJson(jsonString: String): Task[Json] =
    Task.fromEither(parse(jsonString))

  private def validateSelfDescribingJsonAndExtractData(json: Json): Task[Json] =
    for {
      manifest <- parseSelfDescribingData(json)
      client   = Client[Task, Json](Resolver(List(EmbeddedRegistry), None), CirceValidator)
      _        <- client.check(manifest).leftMap(e => new RuntimeException(e.getMessage)).value.absolve
    } yield manifest.data

  private def getUsedSchemas(jsonData: Json): Task[List[Schema.Metadata]] =
    Task.fromEither(jsonData.hcursor.get[List[Schema.Metadata]]("schemas"))

  private def parseSelfDescribingData(jsonData: Json): Task[SelfDescribingData[Json]] =
    Task.fromEither(
      SelfDescribingData
        .parse(jsonData)
        .leftMap(e => new RuntimeException(s"Manifest parsing failed: ${e.code}"))
    )

  private def readFileToString(path: String): Task[String] = {
    val openFile: String => Task[BufferedSource] = path => Task.effect(Source.fromFile(path, "UTF-8"))
    val closeFile: BufferedSource => UIO[Unit]   = bs => Task.effect(bs.close()).ignore

    openFile(path).bracket(closeFile) { file =>
      Task.effect(file.getLines.mkString)
    }
  }
}
