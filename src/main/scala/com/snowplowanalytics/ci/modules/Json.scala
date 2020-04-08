package com.snowplowanalytics.ci.modules

import com.snowplowanalytics.ci.modules.SchemaApiClient.Schema
import io.circe._
import io.circe.generic.auto._
import io.circe.parser._
import zio._

import scala.io.{BufferedSource, Source}

object Json {
  def extractUsedSchemasFromManifest(path: String): Task[List[Schema.Metadata]] =
    readFileToString(path) >>= parseJson >>= validateSelfDescribingJsonAndExtractData >>= getUsedSchemas

  private def readFileToString(path: String): Task[String] = {
    val openFile: String => Task[BufferedSource] = path => Task.effect(Source.fromFile(path, "UTF-8"))
    val closeFile: BufferedSource => UIO[Unit]   = bs => Task.effect(bs.close()).ignore

    openFile(path).bracket(closeFile) { file =>
      Task.effect(file.getLines.mkString)
    }
  }

  private def parseJson(jsonString: String): Task[Json] =
    Task.fromEither(parse(jsonString))

  // TODO:
  //   - extract "schema" and fetch schema
  //   - extract "data"
  //   - validate "data" against "schema"
  //   - return data
  private def validateSelfDescribingJsonAndExtractData(json: Json): Task[Json] =
    Task.fromEither(json.hcursor.get[Json]("data"))

  private def getUsedSchemas(jsonData: Json): Task[List[Schema.Metadata]] =
    Task.fromEither(jsonData.hcursor.get[List[Schema.Metadata]]("schemas"))
}
