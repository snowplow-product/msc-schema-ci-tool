package com.snowplowanalytics.schemaci.modules

import com.snowplowanalytics.schemaci.modules.Http.SttpImpl
import io.circe.{Decoder, Encoder, Printer}
import io.circe.parser.parse
import sttp.client.{NothingT, Request, Response, StringBody}
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.testing.SttpBackendStub
import sttp.model.{MediaType, Method}
import sttp.model.StatusCode.{BadRequest, NotFound}
import zio.{Task, ULayer, ZLayer}

object TestFixtures {

  def sttpBackendStubForPost[A: Decoder, B: Encoder](
    requestMatcher: Request[_, _] => Boolean,
    answer: A => Response[B]
  ): SttpBackendStub[Task, Nothing] =
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
            case _ => Response("{}", BadRequest)
          }
        case _ => Response("{}", NotFound)
      }

  def sttpBackendStubForGet[B: Encoder](
    requestMatcher: Request[_, _] => Boolean,
    answer: Response[B]
  ): SttpBackendStub[Task, Nothing] =
    AsyncHttpClientZioBackend
      .stub
      .whenRequestMatchesPartial {
        case r if r.method == Method.GET && requestMatcher(r) =>
          answer.copy(body = Encoder[B].apply(answer.body).printWith(Printer.noSpaces))
        case _                      =>
          Response("{}", NotFound)
      }

  def httpLayerFromSttpStub(sttpBackendStub: SttpBackendStub[Task, Nothing]): ULayer[Http] =
    ZLayer.succeed(new SttpImpl[NothingT](sttpBackendStub))

}
