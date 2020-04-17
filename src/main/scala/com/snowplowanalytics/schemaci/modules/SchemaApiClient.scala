package com.snowplowanalytics.schemaci.modules

import java.security.MessageDigest

import cats.data.NonEmptyList
import cats.implicits._
import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import io.circe._
import io.circe.generic.auto._
import sttp.client.{Request, _}
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe._
import sttp.model.Uri
import zio.{IO, RIO, ZIO}
import zio.console._

object SchemaApiClient {
  def validateSchema(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      schema: Json
  ): RIO[SttpClient, Option[NonEmptyList[String]]] = {
    val buildRequest: Uri => Request[Either[String, String], Nothing] =
      basicRequest.auth
        .bearer(token)
        .body(
          Schema.ValidationRequest(
            Schema.Meta(hidden = false, "entity", Json.fromJsonObject(JsonObject.empty)),
            schema
          )
        )
        .post(_)

    val extractEventualErrors: Json => Either[ParsingError, Option[NonEmptyList[String]]] = { body =>
      val extractNonEmptyErrorsOnFailure: Either[ParsingError, NonEmptyList[String]] = body.hcursor
        .get[List[String]]("errors")
        .leftMap(ParsingError("Cannot extract 'errors' from response", _))
        .flatMap { errors =>
          Either.fromOption(errors.toNel, ParsingError("Operation was not successful, but errors array is empty"))
        }

      body.hcursor
        .get[Boolean]("success")
        .leftMap(ParsingError("Cannot extract 'success' from response", _))
        .ifM(none.asRight, extractNonEmptyErrorsOnFailure.map(_.some))
    }

    for {
      uri         <- parseUri(s"$apiBaseUrl/api/schemas/v1/organizations/$organizationId/validation-requests/sync")
      maybeErrors <- HttpClient.sendRequest(buildRequest(uri), extractEventualErrors)
    } yield maybeErrors
  }

  def checkSchemaDeployment(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      environment: String,
      schemaMetadata: Schema.Key
  ): ZIO[SttpClient with Console, CliError, Boolean] = {
    val basePath = s"$apiBaseUrl/api/schemas/v1"
    val filters  = s"env=$environment&version=${schemaMetadata.version}"

    val extractDeployments: Json => Either[ParsingError, List[Json]] =
      _.hcursor.as[List[Json]].leftMap(ParsingError("Unexpected response format", _))

    for {
      schemaHash  <- computeSchemaHash(organizationId, schemaMetadata)
      uri         <- parseUri(s"$basePath/organizations/$organizationId/schemas/$schemaHash/deployments?$filters")
      deployments <- HttpClient.sendRequest(basicRequest.auth.bearer(token).post(uri), extractDeployments)
    } yield deployments.nonEmpty
  }

  private def parseUri(uri: String): IO[CliError, Uri] =
    ZIO
      .effect(Uri.parse(uri).leftMap(error => CliError.GenericError(s"Uri parsing error: $uri ($error)")))
      .mapError(CliError.GenericError(s"Uri parsing error: $uri", _))
      .absolve

  private def computeSchemaHash(organizationId: String, meta: Schema.Key): IO[CliError, String] =
    ZIO
      .effect(
        MessageDigest
          .getInstance("SHA-256")
          .digest(s"$organizationId-${meta.vendor}-${meta.name}-${meta.format}".getBytes("UTF-8"))
          .map("%02x".format(_))
          .mkString
      )
      .mapError(CliError.GenericError("Hashing error", _))
}
