package com.snowplowanalytics.schemaci

import cats.effect.ExitCode
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.console.putStrLn
import zio.interop.catz._

import com.snowplowanalytics.schemaci.BuildInfo._
import com.snowplowanalytics.schemaci.modules._

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    CommandIOApp
      .run(name, description, version = version.some)(allSubcommands.map(_.leftWiden[Throwable]), args)
      .provideCustomLayer(wireCliEnv)
      .map(_.code)
      .catchAll(t => putStrLn(Console.RED + t.getMessage + Console.RESET).as(1))

  def wireCliEnv: ULayer[SchemaApi with Jwt with Json] = {
    val http      = AsyncHttpClientZioBackend.layer().orDie >>> Http.sttpLayer[WebSocketHandler]
    val schemaApi = http >>> SchemaApi.liveLayer
    val jwt       = http >>> Jwt.auth0Layer
    val json      = Json.circeLayer

    schemaApi ++ jwt ++ json
  }

  def allSubcommands: Opts[CliTask[ExitCode]] =
    Cli.Subcommands.checkDeployments.map(_.process.leftWiden)

}
