package com.snowplowanalytics.datastructures.ci

import io.circe.parser.parse
import io.circe.{Decoder, Encoder, Printer}
import sttp.client.asynchttpclient.WebSocketHandler
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.testing.SttpBackendStub
import sttp.client.{NothingT, Request, Response, StringBody}
import sttp.model.StatusCode.{BadRequest, NotFound}
import sttp.model.{MediaType, Method}
import zio.stream.Stream
import zio.{Task, ULayer, ZLayer}

import com.snowplowanalytics.datastructures.ci.modules.Http
import com.snowplowanalytics.datastructures.ci.modules.Http.SttpImpl

object TestFixtures {

  def sttpBackendStubForPost[A: Decoder, B: Encoder](
      requestMatcher: Request[_, _] => Boolean,
      answer: A => Response[B]
  ): SttpBackendStub[Task, Stream[Throwable, Byte], WebSocketHandler] =
    AsyncHttpClientZioBackend
      .stub
      .whenRequestMatchesPartial {
        case r if r.method == Method.POST && requestMatcher(r) =>
          r.body match {
            case StringBody(body, "utf-8", Some(MediaType.ApplicationJson)) =>
              parse(body)
                .flatMap(_.hcursor.as[A])
                .map(answer.andThen(res => res.copy(body = Encoder[B].apply(res.body).printWith(Printer.noSpaces))))
                .fold(_ => Response("{}", BadRequest), identity)
            case _                                                          => Response("{}", BadRequest)
          }
        case _                                                 => Response("{}", NotFound)
      }

  def sttpBackendStubForGet[B: Encoder](
      requestMatcher: Request[_, _] => Boolean,
      answer: Response[B]
  ): SttpBackendStub[Task, Stream[Throwable, Byte], WebSocketHandler] =
    AsyncHttpClientZioBackend
      .stub
      .whenRequestMatchesPartial {
        case r if r.method == Method.GET && requestMatcher(r) =>
          answer.copy(body = Encoder[B].apply(answer.body).printWith(Printer.noSpaces))
        case _                                                =>
          Response("{}", NotFound)
      }

  def httpLayerFromSttpStub(
      sttpBackendStub: SttpBackendStub[Task, Stream[Throwable, Byte], WebSocketHandler]
  ): ULayer[Http] =
    ZLayer.succeed(new SttpImpl[NothingT](sttpBackendStub))

}
