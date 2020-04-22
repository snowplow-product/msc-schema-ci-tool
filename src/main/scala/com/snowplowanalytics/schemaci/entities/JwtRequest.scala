package com.snowplowanalytics.schemaci.entities

import io.circe.Encoder
import io.circe.generic.extras.Configuration
import io.circe.generic.extras.semiauto.deriveConfiguredEncoder

case class JwtRequest(
    clientId: String,
    clientSecret: String,
    audience: String,
    grantType: String,
    username: String,
    password: String
)
object JwtRequest {
  implicit val customConfig: Configuration       = Configuration.default.withSnakeCaseMemberNames
  implicit val snakyEncoder: Encoder[JwtRequest] = deriveConfiguredEncoder
}
