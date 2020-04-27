package com.snowplowanalytics.schemaci

import cats.implicits._
import com.github.j5ik2o.base64scala.Base64String
import com.monovore.decline.Opts
import com.snowplowanalytics.schemaci.commands.CheckDeployments
import eu.timepit.refined.auto._

object Cli {

  object Envs {

    // Common environment variables
    val apiBaseUrl: Opts[URL] = Opts
      .env[String]("API_BASE_URL", "Snowplow API base url", "URL")
      .refineToUrl
      .withDefault("https://console.snowplowanalytics.com")

    val authServerBaseUrl: Opts[URL] = Opts
      .env[String]("AUTH_SERVER_BASE_URL", "Authentication server base url", "URL")
      .refineToUrl
      .withDefault("https://id.snowplowanalytics.com")

    val authClientId: Opts[String] = Opts
      .env[String]("AUTH_CLIENT_ID", "Client Id of the registered OAuth2 app", "string")
      .withDefault(decode(BuildInfo.cid))

    val authClientSecret: Opts[String] = Opts
      .env[String]("AUTH_CLIENT_SECRET", "Client Secret of the registered OAuth2 app", "string")
      .withDefault(decode(BuildInfo.cs))

    val authAudience: Opts[URL] = Opts
      .env[String]("AUTH_AUDIENCE", "Audience of the registered OAuth2 app", "URL")
      .refineToUrl
      .withDefault("https://snowplowanalytics.com/api/") // This should be set when Auth0 client gets created on Prod
  }

  object Options {

    // Common options
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

    val checkDeployments: Opts[CheckDeployments] =
      Opts.subcommand("check", "Verify that all schema dependencies are deployed to a particular environment") {
        (
          Options.manifestPath,
          Options.username,
          Options.password,
          Options.environment,
          Envs.apiBaseUrl,
          Envs.authServerBaseUrl,
          Envs.authClientId,
          Envs.authClientSecret,
          Envs.authAudience
        ).mapN(CheckDeployments.apply)
      }

  }

  private val decode: String => String = Base64String(_, urlSafe = true).decodeToString.right.get
}
