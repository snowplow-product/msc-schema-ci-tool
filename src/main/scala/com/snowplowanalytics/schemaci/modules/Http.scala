package com.snowplowanalytics.schemaci.modules

import cats.syntax.either._
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import io.circe.{Json => CJson}
import izumi.reflect.Tags.TagK
import sttp.client.{Request, SttpBackend}
import sttp.client.circe.asJsonAlways
import zio._

object Http {
  type SttpRequest = Request[Either[String, String], Nothing]

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
  final class SttpImpl[WS[_]](sttpBackend: SttpBackend[Task, Nothing, WS]) extends Service {

    override def sendRequest[A](request: SttpRequest, extractor: CJson => Either[ParsingError, A]): IO[CliError, A] =
      sendRequest(request)
        .map(payload => extractor(payload))
        .absolve

    override def sendRequest(request: SttpRequest): IO[CliError, CJson] =
      sttpBackend
        .send(request.response(asJsonAlways[CJson]))
        .bimap(
          CliError.GenericError("Network error", _),
          _.body.leftMap(de => CliError.Json.ParsingError("Cannot decode response payload as JSON", de.error))
        )
        .absolve

  }

  // layers
  def sttpLayer[WS[_]: TagK]: URLayer[SttpClient[WS], Http] = ZLayer.fromService(new SttpImpl[WS](_))
}
