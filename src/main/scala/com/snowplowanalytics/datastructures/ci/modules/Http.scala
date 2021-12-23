package com.snowplowanalytics.datastructures.ci.modules

import cats.syntax.either._
import io.circe.{Json => CJson}
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.asynchttpclient.zio.SttpClient
import sttp.client3.circe.asJsonAlways
import sttp.client3.{Request, SttpBackend}
import zio._

import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.errors.CliError.Json.ParsingError

object Http {
  type SttpRequest = Request[Either[String, String], Any]

  trait Service {
    def sendRequest[A](request: SttpRequest, extractor: CJson => Either[ParsingError, A]): IO[CliError, A]

    def sendRequest(request: SttpRequest): IO[CliError, CJson]
  }

  // accessors
  def sendRequest[A](request: SttpRequest, extractor: CJson => Either[ParsingError, A]): ZIO[Http, CliError, A] =
    ZIO.accessM(_.get[Http.Service].sendRequest[A](request, extractor))

  def sendRequest(request: SttpRequest): ZIO[Http, CliError, CJson] =
    ZIO.accessM(_.get[Http.Service].sendRequest(request))

  // implementations
  final class SttpImpl(sttpBackend: SttpBackend[Task, ZioStreams with WebSockets]) extends Service {

    override def sendRequest[A](request: SttpRequest, extractor: CJson => Either[ParsingError, A]): IO[CliError, A] =
      sendRequest(request)
        .map(payload => extractor(payload))
        .absolve

    override def sendRequest(request: SttpRequest): IO[CliError, CJson] =
      sttpBackend
        .send(request.response(asJsonAlways[CJson]))
        .mapBoth(
          CliError.GenericError("Network error", _),
          _.body.leftMap(de => CliError.Json.ParsingError("Cannot decode response payload as JSON", de.error))
        )
        .absolve

  }

  // layers
  def sttpLayer: URLayer[SttpClient, Http] = ZLayer.fromService(new SttpImpl(_))
}
