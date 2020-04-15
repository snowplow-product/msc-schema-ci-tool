package com.snowplowanalytics.schemaci.entities

import cats.Show
import cats.implicits._
import io.circe.Json

object Schema {
  case class Meta(hidden: Boolean, schemaType: String, customData: Json)

  case class ValidationRequest(meta: Meta, data: Json)

  case class Metadata(vendor: String, name: String, format: String, version: String)
  object Metadata {
    implicit val metadataShow: Show[Metadata] =
      Show.show(metadata => {
        s"""|- Vendor:  ${metadata.vendor}
            |  Name:    ${metadata.name}
            |  Format:  ${metadata.format}
            |  Version: ${metadata.version}""".stripMargin
      })
    implicit val metadataListShow: Show[List[Metadata]] =
      Show.show(metaList => metaList.mkString_("\n", "\n\n", "\n"))
  }
}
