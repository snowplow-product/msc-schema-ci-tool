package com.snowplowanalytics.datastructures.ci.modules

import cats.syntax.either._
import io.circe.literal._
import io.circe.{Json => CJson}
import sttp.client3._
import sttp.client3.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client3.testing.SttpBackendStub
import zio.{Task, ZIO}
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment
import com.snowplowanalytics.datastructures.ci.TestFixtures._
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.errors.CliError.Json.ParsingError
import com.snowplowanalytics.datastructures.ci.modules.Http.sendRequest
import sttp.capabilities.zio.ZioStreams
import sttp.capabilities.WebSockets

object HttpSpec extends DefaultRunnableSpec {

  private val throwingBackend: SttpBackendStub[Task, ZioStreams with WebSockets] =
    AsyncHttpClientZioBackend
      .stub
      .whenAnyRequest
      .thenRespondF(req => ZIO.fail(new SttpClientException.ConnectException(req, new RuntimeException)))

  private val nonJsonBackend: SttpBackendStub[Task, ZioStreams with WebSockets] =
    AsyncHttpClientZioBackend
      .stub
      .whenAnyRequest
      .thenRespond("plaintext")

  private val jsonBackend: SttpBackendStub[Task, ZioStreams with WebSockets] =
    sttpBackendStubForGet(_ => true, Response.ok(json"""{ "key": "value" }"""))

  override def spec: ZSpec[TestEnvironment, Any] =
    suite("Http spec")(
      suite("sendRequest")(
        testM("should fail when backend fails to send request / receive response") {
          val request = basicRequest.get(uri"https://example.com")
          assertM(
            sendRequest(request).run.provideCustomLayer(httpLayerFromSttpStub(throwingBackend))
          )(
            fails(isSubtype[CliError.GenericError](anything))
          )
        },
        testM("should fail when server's response (either 2xx or not) cannot be deserialized as JSON") {
          val request = basicRequest.get(uri"https://example.com")
          assertM(
            sendRequest(request).run.provideCustomLayer(httpLayerFromSttpStub(nonJsonBackend))
          )(
            fails(isSubtype[CliError.Json.ParsingError](anything))
          )
        },
        testM("should be able to deserialize portions of response given an extractor") {
          val request                                          = basicRequest.get(uri"https://example.com")
          val extractor: CJson => Either[ParsingError, String] =
            _.hcursor.get[String]("key").leftMap(CliError.Json.ParsingError("fail", _))
          assertM(
            sendRequest(request, extractor).provideCustomLayer(httpLayerFromSttpStub(jsonBackend))
          )(
            equalTo("value")
          )
        }
      )
    )

}
