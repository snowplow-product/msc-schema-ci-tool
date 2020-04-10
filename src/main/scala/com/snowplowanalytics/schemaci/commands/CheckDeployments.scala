package com.snowplowanalytics.schemaci.commands

import cats.data.NonEmptyList
import cats.effect.ExitCode
import cats.implicits._
import com.snowplowanalytics.schemaci.modules.JsonProvider.extractUsedSchemasFromManifest
import com.snowplowanalytics.schemaci.modules.JwtProvider.getAccessToken
import com.snowplowanalytics.schemaci.modules.SchemaApiClient.{checkSchemaDeployment, Schema}
import com.snowplowanalytics.schemaci._
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
)
object CheckDeployments {
  def process(
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
  ): CliTask[ExitCode] =
    for {
      schemas <- extractUsedSchemasFromManifest(manifestPath)
      _       <- putStrLn(schemas.mkString(s"Checking into $env environment if these schemas are deployed: ", ", ", ""))
      token   <- getAccessToken(authServerBaseUrl.value, clientId, clientSecret, audience.value, username, password)
      result  <- verifySchemaDeployment(apiBaseUrl.value, token, organizationId.value, env, schemas)
    } yield result

  private def verifySchemaDeployment(
      apiBaseUrl: String,
      token: String,
      organizationId: String,
      environment: String,
      schemas: List[Schema.Metadata]
  ): RIO[SttpClient with Console, ExitCode] =
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
        results => {
          Either
            .fromOption(results.flatten.toNel.map(DeploymentCheckFailure(environment, _)), ())
            .swap
            .fold(
              e => putStrLn(s"Deployment check failed! ${e.getMessage}").as(ExitCode.Error),
              _ => putStrLn("All schemas are deployed! You are good to go.").as(ExitCode.Success)
            )
        }
      )

  case class DeploymentCheckFailure(environment: String, schemas: NonEmptyList[Schema.Metadata]) extends Exception {
    override def getMessage: String =
      schemas
        .map(_.toString)
        .mkString_(s"Some schemas are not deployed on '$environment' environment: ", ", ", "")
  }
}
