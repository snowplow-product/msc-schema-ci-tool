package com.snowplowanalytics

import eu.timepit.refined.api.Refined
import eu.timepit.refined.string.{Url, Uuid}
import sttp.client.asynchttpclient.zio.SttpClient
import zio.RIO
import zio.console.Console

package object schemaci {
  type CliTask[A] = RIO[Console with SttpClient, A]

  type UUID = Refined[String, Uuid]
  type URL  = Refined[String, Url]
}
