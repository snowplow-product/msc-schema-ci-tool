package com.snowplowanalytics.ci.modules

import java.security.MessageDigest

import cats.implicits._
import cats.data.ValidatedNel
import io.circe._
import io.circe.literal._
import sttp.client._
import sttp.client.circe._
import sttp.client.asynchttpclient.zio.SttpClient
import zio.RIO

object SchemaApiClient {
  object Schema {
    case class Metadata(vendor: String, name: String, format: String, version: String)
  }

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
          .body(
            json"""
            {
              "meta": {
                "hidden": false,
                "schemaType": "event",
                "customData": {}
              },
              "data": $schema
            }
            """
          )
          .post(uri"$apiBaseUrl/api/schemas/v1/organizations/$organizationId/validation-requests/sync")
          .response(asJson[Json])
      )
      .map(_.body)
      .absolve
      .map(
        body =>
          for {
            success <- body.hcursor.get[Boolean]("success")
            errors  <- body.hcursor.get[List[String]]("errors")
            errorsNel <- Either.fromOption(
                          errors.toNel,
                          ParsingFailure("Errors should not be empty when success is false", null)
                        )
          } yield Either.cond(success, (), errorsNel).toValidated
      )
      .absolve

  def checkSchemaDeployment(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      environment: String,
      schemaMetadata: Schema.Metadata
  ): RIO[SttpClient, Boolean] =
    SttpClient
      .send(
        basicRequest.auth
          .bearer(token)
          .post(
            uri"$apiBaseUrl/api/schemas/v1/organizations/$organizationId/schemas/${computeSchemaHash(organizationId, schemaMetadata)}/deployments?env=$environment&version=${schemaMetadata.version}"
          )
          .response(asJson[Json])
      )
      .map(_.body)
      .absolve
      .map(_.hcursor.as[List[Json]])
      .absolve
      .map(_.nonEmpty)

  private def computeSchemaHash(organizationId: String, meta: Schema.Metadata): String =
    MessageDigest
      .getInstance("SHA-256")
      .digest(s"$organizationId-${meta.vendor}-${meta.name}-${meta.format}".getBytes("UTF-8"))
      .map("%02x".format(_))
      .mkString
}
