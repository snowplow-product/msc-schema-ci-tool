package com.snowplowanalytics

import cats.implicits._
import com.monovore.decline.Opts
import eu.timepit.refined._
import eu.timepit.refined.api.{Refined, Validate}
import eu.timepit.refined.string.{Url, Uuid}
import zio.ZIO
import zio.console.Console

import com.snowplowanalytics.schemaci.errors.CliError
import com.snowplowanalytics.schemaci.modules._

package object schemaci {
  type CliEnv     = Console with Json with Jwt with SchemaApi
  type CliTask[A] = ZIO[CliEnv, CliError, A]

  type UUID = Refined[String, Uuid]
  type URL  = Refined[String, Url]

  implicit class refinedSyntax[A](opts: Opts[A]) {

    def refine[P](message: A => String)(implicit V: Validate[A, P]): Opts[Refined[A, P]] =
      opts.mapValidated { v =>
        val erroredOpts = opts.toString().stripPrefix("Opts(").stripSuffix(")")
        val maybePath   = Option(erroredOpts).filter(_.nonEmpty).map(" [" + _ + "]: ").getOrElse(": ")
        refineV[P](v).leftMap(_ => "Error while parsing" + maybePath + message(v)).toValidatedNel
      }

    def refineToUrl(implicit V: Validate[A, Url]): Opts[Refined[A, Url]] =
      opts.refine[Url](value => s"'$value' is not a valid URL")

    def refineToUuid(implicit V: Validate[A, Uuid]): Opts[Refined[A, Uuid]] =
      opts.refine[Uuid](value => s"'$value' is not a valid UUID")

  }

}
