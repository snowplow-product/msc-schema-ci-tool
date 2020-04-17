package com.snowplowanalytics.schemaci.modules

import cats.syntax.either._
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import io.circe.Json
import sttp.client.Request
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe.asJsonAlways
import zio.ZIO

object HttpClient {
  def sendRequest[A](
      request: Request[Either[String, String], Nothing],
      extractor: Json => Either[ParsingError, A]
  ): ZIO[SttpClient, CliError, A] =
    sendRequest(request)
      .map(payload => extractor(payload))
      .absolve

  def sendRequest(request: Request[Either[String, String], Nothing]): ZIO[SttpClient, CliError, Json] =
    SttpClient
      .send(request.response(asJsonAlways[Json]))
      .mapError(CliError.GenericError("Network error", _))
      .map(_.body.leftMap(de => CliError.Json.ParsingError("Cannot decode response payload as JSON", de.error)))
      .absolve
}
