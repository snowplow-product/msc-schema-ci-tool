package com.snowplowanalytics.schemaci.commands

import cats.effect.ExitCode
import cats.implicits._
import com.snowplowanalytics.schemaci.modules.JsonProvider.extractUsedSchemasFromManifest
import com.snowplowanalytics.schemaci.modules.JwtProvider.getAccessToken
import com.snowplowanalytics.schemaci.modules.SchemaApiClient.checkSchemaDeployment
import com.snowplowanalytics.schemaci._
import com.snowplowanalytics.schemaci.entities.Schema
import com.snowplowanalytics.schemaci.entities.Schema.Metadata._
import com.snowplowanalytics.schemaci.errors.DeploymentCheckFailure
import sttp.client.asynchttpclient.zio.SttpClient
import zio._
import zio.console._

case class CheckDeployments(
    manifestPath: String,
    organizationId: UUID,
    clientId: String,
    clientSecret: String,
    audience: URL,
    username: String,
    password: String,
    environment: String,
    authServerBaseUrl: URL,
    apiBaseUrl: URL
) extends CliSubcommand {
  override def process: CliTask[ExitCode] = {
    val printInfo: List[Schema.Metadata] => RIO[Console, Unit] = schemas =>
      for {
        _ <- putStrLn(s"Ensuring that the following schemas are already deployed on '$environment':")
        _ <- putStrLn(s"${schemas.show}")
      } yield ()

    val printSuccess: RIO[Console, Unit] =
      putStrLn("All schemas are already deployed! You are good to go.")

    val printError: DeploymentCheckFailure => RIO[Console, Unit] = e =>
      for {
        _ <- putStrLn(scala.Console.RED)
        _ <- putStrLn("Deployment check failed!")
        _ <- putStrLn(s"The following schemas are not deployed on '$environment' yet:")
        _ <- putStrLn(s"${e.schemas.toList.show}")
        _ <- putStrLn(scala.Console.RESET)
      } yield ()

    for {
      schemas  <- extractUsedSchemasFromManifest(manifestPath)
      _        <- printInfo(schemas)
      token    <- getAccessToken(authServerBaseUrl.value, clientId, clientSecret, audience.value, username, password)
      result   <- verifySchemaDeployment(apiBaseUrl.value, token, organizationId.value, environment, schemas)
      exitCode <- result.fold(printSuccess.as(ExitCode.Success))(printError(_).as(ExitCode.Error))
    } yield exitCode
  }

  private def verifySchemaDeployment(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      environment: String,
      schemas: List[Schema.Metadata]
  ): RIO[SttpClient with Console, Option[DeploymentCheckFailure]] =
    ZIO
      .collectAllParN(5)(
        schemas.map { schema =>
          checkSchemaDeployment(apiBaseUrl, token, organizationId, environment, schema)
            .map(found => Option(schema).filter(_ => !found))
        }
      )
      .map(_.flatten.toNel.map(DeploymentCheckFailure))
}
