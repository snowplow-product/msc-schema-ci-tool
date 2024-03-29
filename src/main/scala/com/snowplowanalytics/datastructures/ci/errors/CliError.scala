package com.snowplowanalytics.datastructures.ci.errors

import cats.syntax.option._
import io.circe

import com.snowplowanalytics.iglu.core.ParseError

sealed abstract class CliError extends Exception with Product with Serializable {
  def message: String
  def underlying: Option[Throwable]

  override def getMessage: String =
    message + underlying.flatMap(t => Option(t.getMessage)).filterNot(_.isEmpty).fold("")(" (" + _ + ")")

}

object CliError {
  sealed abstract class Auth extends CliError

  object Auth {

    case object InvalidApiKey extends Auth {
      override def message: String               = "Invalid api key"
      override def underlying: Option[Throwable] = none
    }

    case class InvalidToken(underlying: Option[Throwable]) extends Auth {
      override def message: String = "Invalid Bearer token"
    }

  }

  sealed abstract class Json extends CliError

  object Json {
    case class ParsingError(message: String, underlying: Option[Throwable] = none) extends Json

    object ParsingError {

      def apply(message: String, circeError: circe.Error): ParsingError =
        ParsingError(message, circeError.some)

      def apply(message: String, igluError: ParseError): ParsingError =
        ParsingError(message, new Exception(igluError.code).some)

    }

  }

  case class GenericError(message: String, underlying: Option[Throwable]) extends CliError

  object GenericError {
    def apply(message: String): GenericError                        = GenericError(message, none)
    def apply(message: String, underlying: Throwable): GenericError = GenericError(message, underlying.some)
  }

}
