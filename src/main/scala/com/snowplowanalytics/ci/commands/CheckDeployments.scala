package com.snowplowanalytics.ci.commands

import cats.data.NonEmptyList
import cats.implicits._
import com.snowplowanalytics.ci.Commands.{CliTask, URL, UUID}
import com.snowplowanalytics.ci.modules.JsonProvider.extractUsedSchemasFromManifest
import com.snowplowanalytics.ci.modules.JwtProvider.getAccessToken
import com.snowplowanalytics.ci.modules.SchemaApiClient.{checkSchemaDeployment, Schema}
import sttp.client.asynchttpclient.zio.SttpClient
import zio._
import zio.console._

object CheckDeployments {
  def command(
      authServerBaseUrl: URL,
      apiBaseUrl: URL
  )(
      manifestPath: String,
      organizationId: UUID,
      clientId: String,
      clientSecret: String,
      audience: URL,
      username: String,
      password: String,
      env: String
  ): CliTask =
    for {
      schemas <- extractUsedSchemasFromManifest(manifestPath)
      _       <- putStrLn(schemas.mkString(s"Checking into $env environment if these schemas are deployed: ", ", ", ""))
      token   <- getAccessToken(authServerBaseUrl.value, clientId, clientSecret, audience.value, username, password)
      _       <- verifySchemaDeployment(apiBaseUrl.value, token, organizationId.value, env, schemas)
    } yield ()

  private def verifySchemaDeployment(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      environment: String,
      schemas: List[Schema.Metadata]
  ): RIO[SttpClient with Console, Unit] =
    ZIO
      .collectAllParN(5)(
        schemas
          .map(
            schema =>
              checkSchemaDeployment(apiBaseUrl, token, organizationId, environment, schema)
                .map(found => Option(schema).filter(_ => !found))
          )
      )
      .flatMap(
        results =>
          ZIO
            .fromEither(Either.fromOption(results.flatten.toNel.map(DeploymentCheckFailure(environment, _)), ()).swap)
            .zipRight(putStrLn("All schemas are deployed! You are good to go."))
            .tapError(e => putStrLn(s"Deployment check failed! ${e.getMessage}"))
      )

  case class DeploymentCheckFailure(environment: String, schemas: NonEmptyList[Schema.Metadata]) extends Exception {
    override def getMessage: String =
      schemas
        .map(_.toString)
        .mkString_(s"Some schemas are not deployed on '$environment' environment: ", ", ", "")
  }
}
