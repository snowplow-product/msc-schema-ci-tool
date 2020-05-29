package com.snowplowanalytics.datastructures.ci

import io.circe.{Json => CJson}
import zio.test.mock.{Method, Proxy}
import zio.{Has, IO, URLayer, ZLayer}

import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.DataStructuresApi

object DataStructuresApiMock {

  sealed trait Tag[I, A] extends Method[DataStructuresApi, I, A] {
    def envBuilder = DataStructuresApiMock.envBuilder
  }

  object ValidateSchema        extends Tag[(URL, String, UUID, CJson), Schema.ValidationResponse]
  object CheckSchemaDeployment extends Tag[(URL, String, UUID, String, Schema.Key), Boolean]

  private lazy val envBuilder: URLayer[Has[Proxy], DataStructuresApi] =
    ZLayer.fromService(invoke =>
      new DataStructuresApi.Service {

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
