package com.snowplowanalytics.datastructures.ci

import cats.implicits._
import com.monovore.decline.Opts
import eu.timepit.refined.auto._

import com.snowplowanalytics.datastructures.ci.commands.CheckDeployments

object Cli {

  object Envs {

    // Common environment variables
    val apiBaseUrl: Opts[URL] =
      Opts
        .env[String]("API_BASE_URL", "Snowplow API base url", "URL")
        .refineToUrl
        .withDefault("https://console.snowplowanalytics.com/api/msc/v1")

    val organizationId: Opts[UUID] =
      Opts
        .env[String]("ORGANIZATION_ID", "UUID of the Snowplow organization as found in Snowplow BDP Console", "UUID")
        .refineToUuid

    val apiKey: Opts[String] =
      Opts
        .env[String]("API_KEY", "Snowplow BDP Console API key", "string")

  }

  object Options {

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
          Options.environment,
          Envs.apiBaseUrl,
          Envs.organizationId,
          Envs.apiKey
        ).mapN(CheckDeployments.apply)
      }

  }

}
