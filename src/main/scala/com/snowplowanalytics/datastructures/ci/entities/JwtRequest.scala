package com.snowplowanalytics.datastructures.ci.entities

import io.circe.Codec
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto._

case class JwtRequest(
    clientId: String,
    clientSecret: String,
    audience: String,
    grantType: String,
    username: String,
    password: String
)

object JwtRequest {
  implicit val snaky: Configuration                    = Configuration.default.withSnakeCaseMemberNames
  implicit val snakyJwtRequestCodec: Codec[JwtRequest] = deriveConfiguredCodec
}
