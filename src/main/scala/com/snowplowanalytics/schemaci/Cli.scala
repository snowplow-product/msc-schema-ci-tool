package com.snowplowanalytics.schemaci

import cats.implicits._
import com.monovore.decline.Opts
import com.snowplowanalytics.schemaci.commands.CheckDeployments
import eu.timepit.refined.auto._

object Cli {
  object Envs {
    // Common environment variables
    val authServerBaseUrl: Opts[URL] = Opts
      .env[String]("AUTH_SERVER_BASE_URL", "Authentication server base url", "URL")
      .refineToUrl
      .withDefault("https://id.snowplowanalytics.com")

    val apiBaseUrl: Opts[URL] = Opts
      .env[String]("API_BASE_URL", "Snowplow API base url", "URL")
      .refineToUrl
      .withDefault("https://console.snowplowanalytics.com")
  }

  object Options {
    // Common options
    val organizationId: Opts[UUID] =
      Opts.option[String]("organizationId", "The Organization Id (UUID)", "", "UUID").refineToUuid
    val clientId: Opts[String] =
      Opts.option[String]("clientId", "Client Id of the registered OAuth2 app", "", "string")
    val clientSecret: Opts[String] =
      Opts.option[String]("clientSecret", "Client Secret of the registered OAuth2 app", "", "string")
    val audience: Opts[URL] =
      Opts.option[String]("audience", "Audience of the registered OAuth2 app", "", "URL").refineToUrl
    val username: Opts[String] =
      Opts.option[String]("username", "Username of the CI user", "", "string")
    val password: Opts[String] =
      Opts.option[String]("password", "Password of the CI user", "", "string")

    // Check Deployment options
    val manifestPath: Opts[String] =
      Opts.option[String]("manifestPath", "Path to the schema manifest", "", "path")
    val environment: Opts[String] =
      Opts.option[String]("environment", "Environment into which schemas should be already deployed", "", "string")
  }

  object Subcommands {
    val checkDeployments: Opts[CheckDeployments] = Opts
      .subcommand(
        "check",
        "Verify that all schema dependencies are deployed to a particular environment"
      ) {
        (
          Options.manifestPath,
          Options.organizationId,
          Options.clientId,
          Options.clientSecret,
          Options.audience,
          Options.username,
          Options.password,
          Options.environment,
          Envs.authServerBaseUrl,
          Envs.apiBaseUrl
        ).mapN(CheckDeployments.apply)
      }
  }
}
