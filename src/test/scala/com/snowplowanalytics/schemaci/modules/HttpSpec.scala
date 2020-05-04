package com.snowplowanalytics.schemaci.modules

import cats.syntax.either._
import io.circe.literal._
import io.circe.{Json => CJson}
import sttp.client._
import sttp.client.asynchttpclient.zio.AsyncHttpClientZioBackend
import sttp.client.testing.SttpBackendStub
import zio.Task
import zio.test.Assertion._
import zio.test._
import zio.test.environment.TestEnvironment

import com.snowplowanalytics.schemaci.TestFixtures._
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import com.snowplowanalytics.schemaci.modules.Http.sendRequest

object HttpSpec extends DefaultRunnableSpec {

  private def suspendThrow(e: => Throwable): Response[Any] =
    throw e

  private val throwingBackend: SttpBackendStub[Task, Nothing] =
    AsyncHttpClientZioBackend
      .stub
      .whenAnyRequest
      .thenRespond(suspendThrow(new SttpClientException.ConnectException(new RuntimeException)))

  private val nonJsonBackend: SttpBackendStub[Task, Nothing] =
    AsyncHttpClientZioBackend
      .stub
      .whenAnyRequest
      .thenRespond("plaintext")

  private val jsonBackend: SttpBackendStub[Task, Nothing] =
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
          val request = basicRequest.get(uri"https://example.com")
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
