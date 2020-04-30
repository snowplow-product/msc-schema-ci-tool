package com.snowplowanalytics.schemaci.modules

import java.security.MessageDigest

import cats.data.NonEmptyList
import cats.implicits._
import com.snowplowanalytics.schemaci.{URL, UUID}
import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.entities.Schema.ValidationResponse
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.errors.CliError.Json.ParsingError
import com.snowplowanalytics.schemaci.modules.Http.SttpRequest
import io.circe.{Json => CJson, _}
import io.circe.generic.auto._
import sttp.client._
import sttp.client.circe._
import sttp.model.Uri
import zio.{IO, URLayer, ZIO, ZLayer}

object SchemaApi {

  trait Service {

    def validateSchema(
      apiBaseUrl: URL,
      token: String,
      organizationId: UUID,
      schema: CJson
    ): IO[CliError, ValidationResponse]

    def checkSchemaDeployment(
      apiBaseUrl: URL,
      token: String,
      organizationId: UUID,
      environment: String,
      schemaMetadata: Schema.Key
    ): IO[CliError, Boolean]

  }

  // accessors
  def validateSchema(
    apiBaseUrl: URL,
    token: String,
    organizationId: UUID,
    schema: CJson
  ): ZIO[SchemaApi, CliError, ValidationResponse] =
    ZIO.accessM(_.get[SchemaApi.Service].validateSchema(apiBaseUrl, token, organizationId, schema))

  def checkSchemaDeployment(
    apiBaseUrl: URL,
    token: String,
    organizationId: UUID,
    environment: String,
    schemaMetadata: Schema.Key
  ): ZIO[SchemaApi, CliError, Boolean] =
    ZIO.accessM(
      _.get[SchemaApi.Service].checkSchemaDeployment(apiBaseUrl, token, organizationId, environment, schemaMetadata)
    )

  // implementations
  final class LiveImpl(http: Http.Service) extends Service {

    override def validateSchema(
      apiBaseUrl: URL,
      token: String,
      organizationId: UUID,
      schema: CJson
    ): IO[CliError, ValidationResponse] = {
      val buildRequest: Uri => SttpRequest =
        basicRequest
          .auth
          .bearer(token)
          .body(
            Schema.ValidationRequest(
              Schema.Meta(hidden = false, "entity", CJson.fromJsonObject(JsonObject.empty)),
              schema
            )
          )
          .post(_)

      val extractEventualErrorsAndWarnings: CJson => Either[ParsingError, ValidationResponse] = { body =>
        val extractNonEmptyErrors: Either[ParsingError, NonEmptyList[String]] =
          body
            .hcursor
            .get[List[String]]("errors")
            .leftMap(ParsingError("Cannot extract 'errors' from response", _))
            .flatMap { errors =>
              Either.fromOption(errors.toNel, ParsingError("Operation was not successful, but errors array is empty"))
            }

        val maybeErrors: Either[ParsingError, Option[NonEmptyList[String]]] =
          body
            .hcursor
            .get[Boolean]("success")
            .leftMap(ParsingError("Cannot extract 'success' from response", _))
            .ifM(none.asRight, extractNonEmptyErrors.map(_.some))

        val warnings: Either[ParsingError, List[String]] =
          body
            .hcursor
            .get[List[String]]("warnings")
            .leftMap(ParsingError("Cannot extract 'warnings' from response", _))

        (maybeErrors, warnings).mapN(ValidationResponse)
      }

      for {
        uri         <- parseUri(s"$apiBaseUrl/api/schemas/v1/organizations/$organizationId/validation-requests/sync")
        maybeErrors <- http.sendRequest(buildRequest(uri), extractEventualErrorsAndWarnings)
      } yield maybeErrors
    }

    override def checkSchemaDeployment(
      apiBaseUrl: URL,
      token: String,
      organizationId: UUID,
      environment: String,
      schemaMetadata: Schema.Key
    ): IO[CliError, Boolean] = {
      val basePath = s"$apiBaseUrl/api/schemas/v1"
      val filters  = s"env=$environment&version=${schemaMetadata.version}"

      val extractDeployments: CJson => Either[ParsingError, List[CJson]] =
        _.hcursor.as[List[CJson]].leftMap(ParsingError("Unexpected response format", _))

      for {
        schemaHash  <- computeSchemaHash(organizationId, schemaMetadata)
        uri         <- parseUri(s"$basePath/organizations/$organizationId/schemas/$schemaHash/deployments?$filters")
        deployments <- http.sendRequest(basicRequest.auth.bearer(token).get(uri), extractDeployments)
      } yield deployments.nonEmpty
    }

    private def parseUri(uri: String): IO[CliError, Uri] =
      ZIO
        .effect(Uri.parse(uri).leftMap(error => CliError.GenericError(s"Uri parsing error: $uri ($error)")))
        .mapError(CliError.GenericError(s"Uri parsing error: $uri", _))
        .absolve

    private def computeSchemaHash(organizationId: UUID, meta: Schema.Key): IO[CliError, String] =
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

  // layers
  def liveLayer: URLayer[Http, SchemaApi] = ZLayer.fromService(new LiveImpl(_))

}
