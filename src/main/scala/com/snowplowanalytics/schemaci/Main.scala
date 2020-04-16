package com.snowplowanalytics.schemaci

import cats.effect.ExitCode
import cats.implicits._
import com.monovore.decline.effect.CommandIOApp
import com.monovore.decline.Opts
import com.snowplowanalytics.schemaci.BuildInfo._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.console.putStrLn
import zio.interop.catz._

object Main extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    CommandIOApp
      .run(name, description, version = version.some)(allSubcommands, args)
      .provideCustomLayer(AsyncHttpClientZioBackend.layer().orDie)
      .map(_.code)
      .catchAll(t => putStrLn(Console.RED + t.getMessage + Console.RESET).as(1))

  val allSubcommands: Opts[CliTask[ExitCode]] = Cli.Subcommands.checkDeployments.map(_.process)
}
