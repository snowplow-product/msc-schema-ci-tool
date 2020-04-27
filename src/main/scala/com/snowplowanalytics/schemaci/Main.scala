package com.snowplowanalytics.schemaci

import cats.effect.ExitCode
import cats.implicits._
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import com.snowplowanalytics.schemaci.BuildInfo._
import com.snowplowanalytics.schemaci.modules._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.console.putStrLn
import zio.interop.catz._

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    CommandIOApp
      .run(name, description, version = version.some)(allSubcommands.map(_.leftWiden[Throwable]), args)
      .provideCustomLayer(wireCliEnv.orDie)
      .map(_.code)
      .catchAll(t => putStrLn(Console.RED + t.getMessage + Console.RESET).as(1))

  def wireCliEnv: TaskLayer[SchemaApi with Jwt with Json] =
    AsyncHttpClientZioBackend.layer() >>> Http.sttpLayer >>> (SchemaApi.liveLayer ++ Jwt.auth0Layer ++ Json.circeLayer)

  def allSubcommands: Opts[CliTask[ExitCode]] =
    Cli.Subcommands.checkDeployments.map(_.process.leftWiden)

}
