package com.snowplowanalytics.datastructures.ci.entities

import cats.Show
import cats.data.NonEmptyList
import cats.implicits._
import io.circe.Json

object Schema {
  case class Meta(hidden: Boolean, schemaType: String, customData: Json)

  case class ValidationRequest(meta: Meta, data: Json)
  case class ValidationResponse(errors: Option[NonEmptyList[String]], warnings: List[String])

  case class Key(vendor: String, name: String, format: String, version: String)

  object Key {

    implicit val metadataShow: Show[Key] =
      Show.show { metadata =>
        s"""- Vendor:  ${metadata.vendor}
           |  Name:    ${metadata.name}
           |  Format:  ${metadata.format}
           |  Version: ${metadata.version}""".stripMargin
      }

    implicit val metadataListShow: Show[List[Key]] =
      Show.show(metaList => metaList.mkString_("\n", "\n\n", "\n"))

  }

}
