package com.snowplowanalytics.ci

import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import zio._
import zio.console.putStrLn

object Main extends App {
  override def run(args: List[String]): ZIO[ZEnv, Nothing, Int] =
    Commands.mainCommand
      .parse(args, sys.env)
      .fold(
        help => putStrLn(help.toString()).as(if (help.errors.isEmpty) 0 else 1),
        _.as(0).catchAll(t => putStrLn(t.getMessage).as(1))
      )
      .provideCustomLayer(AsyncHttpClientZioBackend.layer().orDie)
}
