package com.snowplowanalytics.datastructures.ci.commands

import scala.io.Source

import cats.data.NonEmptyList
import cats.effect.ExitCode
import cats.implicits._
import zio._
import zio.console._

import com.snowplowanalytics.datastructures.ci._
import com.snowplowanalytics.datastructures.ci.entities.Schema
import com.snowplowanalytics.datastructures.ci.entities.Schema.Key._
import com.snowplowanalytics.datastructures.ci.errors.CliError
import com.snowplowanalytics.datastructures.ci.modules.DataStructuresApi
import com.snowplowanalytics.datastructures.ci.modules.DataStructuresApi.checkSchemaDeployment
import com.snowplowanalytics.datastructures.ci.modules.Json._
import com.snowplowanalytics.datastructures.ci.modules.Jwt._

case class CheckDeployments(
    manifestPath: String,
    username: String,
    password: String,
    environment: String,
    apiBaseUrl: URL,
    authServerBaseUrl: URL,
    clientId: String,
    clientSecret: String,
    audience: URL
) extends CliSubcommand {

  override def process: CliTask[ExitCode] = {
    val printInfo: List[Schema.Key] => URIO[Console, Unit] = schemas =>
      for {
        _ <- putStrLn(s"Ensuring that the following schemas are already deployed on '$environment':")
        _ <- putStrLn(s"${schemas.show}")
      } yield ()

    val printSuccess: URIO[Console, Unit] =
      putStrLn("All schemas are already deployed! You are good to go.")

    val printError: NonEmptyList[Schema.Key] => URIO[Console, Unit] = e =>
      for {
        _ <- putStrLn(scala.Console.RED)
        _ <- putStrLn("Deployment check failed!")
        _ <- putStrLn(s"The following schemas are not deployed on '$environment' yet:")
        _ <- putStrLn(s"${e.toList.show}")
        _ <- putStrLn(scala.Console.RESET)
      } yield ()

    for {
      schemas  <- extractSchemaDependenciesFromManifest(Source.fromFile(manifestPath))
      _        <- printInfo(schemas)
      token    <- getAccessToken(authServerBaseUrl, clientId, clientSecret, audience, username, password)
      orgId    <- extractOrganizationIdFromToken(authServerBaseUrl, token)
      result   <- verifySchemaDeployment(apiBaseUrl, token, orgId, environment, schemas)
      exitCode <- result.fold(printSuccess.as(ExitCode.Success))(printError(_).as(ExitCode.Error))
    } yield exitCode
  }

  private def verifySchemaDeployment(
      apiBaseUrl: URL,
      token: String,
      organizationId: UUID,
      environment: String,
      schemas: List[Schema.Key]
  ): ZIO[DataStructuresApi, CliError, Option[NonEmptyList[Schema.Key]]] =
    ZIO
      .foreachParN(5)(schemas) { schema =>
        checkSchemaDeployment(apiBaseUrl, token, organizationId, environment, schema)
          .map(found => Option(schema).filter(_ => !found))
      }
      .map(_.flatten.toNel)

}
