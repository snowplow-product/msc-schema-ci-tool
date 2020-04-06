package com.snowplowanalytics.ci

import cats.implicits._
import com.monovore.decline.{Command, Opts}
import com.monovore.decline.refined._
import com.snowplowanalytics.ci.commands.CheckDeployments
import eu.timepit.refined.api.Refined
import eu.timepit.refined.auto._
import eu.timepit.refined.string._
import sttp.client.asynchttpclient.zio.SttpClient
import zio.RIO
import zio.console.Console

object Commands {
  type CliTask = RIO[Console with SttpClient, Unit]

  type UUID = Refined[String, Uuid]
  type URL  = Refined[String, Url]

  val authServerBaseUrl: Opts[URL] = Opts
    .env[URL]("AUTH_SERVER_BASE_URL", help = "Authentication server base url")
    .withDefault[URL]("https://id.snowplowanalytics.com")

  val apiBaseUrl: Opts[URL] = Opts
    .env[URL]("API_BASE_URL", help = "Snowplow API base url")
    .withDefault[URL]("https://console.snowplowanalytics.com")

  // CHECK DEPLOYMENT
  val desc = "Verify that all schemas referenced in the schema manifest are deployed to a particular environment"
  case class CheckDeploymentsInput(
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

  val manifestPath: Opts[String] =
    Opts.option[String]("manifestPath", "Path to the schema manifest", "", "path")
  val organizationId: Opts[UUID] =
    Opts.option[UUID]("organizationId", "The Organization Id (UUID)", "", "UUID")
  val clientId: Opts[String] =
    Opts.option[String]("clientId", "Client Id of the registered OAuth2 app", "", "string")
  val clientSecret: Opts[String] =
    Opts.option[String]("clientSecret", "Client Secret of the registered OAuth2 app", "", "string")
  val audience: Opts[URL] =
    Opts.option[URL]("audience", "Audience of the registered OAuth2 app", "", "URL")
  val username: Opts[String] =
    Opts.option[String]("username", "Username of the CI user", "", "string")
  val password: Opts[String] =
    Opts.option[String]("password", "Password of the CI user", "", "string")
  val environment: Opts[String] =
    Opts.option[String]("environment", "Environment into which schemas should be already deployed", "", "string")

  val checkDeployments: Opts[CheckDeploymentsInput] = Opts.subcommand("check", desc) {
    (
      manifestPath,
      organizationId,
      clientId,
      clientSecret,
      audience,
      username,
      password,
      environment,
      authServerBaseUrl,
      apiBaseUrl
    ).mapN(CheckDeploymentsInput)
  }

  val mainCommand: Command[CliTask] = Command[CliTask](
    "snowplow-ci",
    "A CLI helper tool for common CI/CD scenarios when developing Snowplow Schemas",
    helpFlag = true
  ) {
    checkDeployments.map {
      case CheckDeploymentsInput(mp, o, cid, cs, a, u, p, e, asbu, abu) =>
        CheckDeployments.command(asbu, abu)(mp, o, cid, cs, a, u, p, e)
    }
  }
}
