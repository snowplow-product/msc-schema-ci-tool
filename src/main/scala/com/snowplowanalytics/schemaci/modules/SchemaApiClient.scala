package com.snowplowanalytics.schemaci.modules

import java.security.MessageDigest

import cats.data.ValidatedNel
import cats.implicits._
import com.snowplowanalytics.schemaci.entities.Schema
import io.circe._
import io.circe.generic.auto._
import sttp.client._
import sttp.client.asynchttpclient.zio.SttpClient
import sttp.client.circe._
import sttp.model.Uri
import zio.{RIO, ZIO}

object SchemaApiClient {
  def validateSchema(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      schema: Json
  ): RIO[SttpClient, ValidatedNel[String, Unit]] =
    SttpClient
      .send(
        basicRequest.auth
          .bearer(token)
          .body(Schema.ValidationRequest(Schema.Meta(false, "entity", Json.fromJsonObject(JsonObject.empty)), schema))
          .post(uri"$apiBaseUrl/api/schemas/v1/organizations/$organizationId/validation-requests/sync")
          .response(asJson[Json])
      )
      .map(_.body)
      .absolve
      .map { body =>
        for {
          success <- body.hcursor.get[Boolean]("success")
          errors  <- body.hcursor.get[List[String]]("errors")
          errorsNel <- Either.fromOption(
                        errors.toNel,
                        ParsingFailure("Errors should not be empty when success is false", null)
                      )
        } yield Either.cond(success, (), errorsNel).toValidated
      }
      .absolve

  def checkSchemaDeployment(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      environment: String,
      schemaMetadata: Schema.Metadata
  ): RIO[SttpClient, Boolean] = {
    val basePath               = "api/schemas/v1"
    val organizations          = s"organizations/$organizationId"
    val schemas                = s"schemas/${computeSchemaHash(organizationId, schemaMetadata)}"
    val deploymentsWithFilters = s"deployments?env=$environment&version=${schemaMetadata.version}"

    ZIO
      .effect(
        Uri
          .parse(s"$apiBaseUrl/$basePath/$organizations/$schemas/$deploymentsWithFilters")
          .leftMap(new RuntimeException(_))
      )
      .absolve
      .flatMap { uri =>
        SttpClient
          .send(
            basicRequest.auth
              .bearer(token)
              .post(uri)
              .response(asJson[Json])
          )
      }
      .map(_.body)
      .absolve
      .map(_.hcursor.as[List[Json]])
      .absolve
      .map(_.nonEmpty)
  }

  private def computeSchemaHash(organizationId: String, meta: Schema.Metadata): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(s"$organizationId-${meta.vendor}-${meta.name}-${meta.format}".getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
}
