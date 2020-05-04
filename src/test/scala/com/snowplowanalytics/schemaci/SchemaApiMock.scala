package com.snowplowanalytics.schemaci

import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.modules.SchemaApi
import io.circe.{Json => CJson}
import zio.{Has, IO, URLayer, ZLayer}
import zio.test.mock.{Method, Proxy}

object SchemaApiMock {

  sealed trait Tag[I, A] extends Method[SchemaApi, I, A] {
    def envBuilder = SchemaApiMock.envBuilder
  }

  object ValidateSchema        extends Tag[(URL, String, UUID, CJson), Schema.ValidationResponse]
  object CheckSchemaDeployment extends Tag[(URL, String, UUID, String, Schema.Key), Boolean]

  private lazy val envBuilder: URLayer[Has[Proxy], SchemaApi] =
    ZLayer.fromService(invoke =>
      new SchemaApi.Service {

        override def validateSchema(
          apiBaseUrl: URL,
          token: String,
          organizationId: UUID,
          schema: CJson
        ): IO[CliError, Schema.ValidationResponse] =
          invoke(ValidateSchema, apiBaseUrl, token, organizationId, schema)

        override def checkSchemaDeployment(
          apiBaseUrl: URL,
          token: String,
          organizationId: UUID,
          environment: String,
          schemaMetadata: Schema.Key
        ): IO[CliError, Boolean] =
          invoke(CheckSchemaDeployment, apiBaseUrl, token, organizationId, environment, schemaMetadata)

      }
    )

}
