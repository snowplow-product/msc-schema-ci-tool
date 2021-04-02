package com.snowplowanalytics.datastructures.ci

import cats.effect.{ExitCode => CEExitCode}
import cats.implicits._
import com.monovore.decline.Opts
import com.monovore.decline.effect.CommandIOApp
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.console.putStrLn
import zio.interop.catz._

import com.snowplowanalytics.datastructures.ci.BuildInfo._
import com.snowplowanalytics.datastructures.ci.modules._

object Main extends App {

  override def run(args: List[String]): ZIO[ZEnv, Nothing, ExitCode] =
    CommandIOApp
      .run(name, description, version = version.some)(allSubcommands.map(_.leftWiden[Throwable]), args)
      .provideCustomLayer(wireCliEnv)
      .map(res => ExitCode(res.code))
      .catchAll(t => putStrLn(Console.RED + t.getMessage + Console.RESET).as(ExitCode.failure))

  def wireCliEnv: ULayer[DataStructuresApi with Jwt with Json] = {
    val http              = AsyncHttpClientZioBackend.layer().orDie >>> Http.sttpLayer
    val dataStructuresApi = http >>> DataStructuresApi.liveLayer
    val jwt               = http >>> Jwt.auth0Layer
    val json              = Json.circeLayer

    dataStructuresApi ++ jwt ++ json
  }

  def allSubcommands: Opts[CliTask[CEExitCode]] =
    Cli.Subcommands.checkDeployments.map(_.process.leftWiden)

}
