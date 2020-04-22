package com.snowplowanalytics.schemaci.entities

import pdi.jwt.JwtAlgorithm
import io.circe._
import io.circe.generic.semiauto._

case class JwkKey(alg: JwtAlgorithm, kid: String, x5c: String)
object JwkKey {
  implicit val jwtAlgorithmDecoder: Decoder[JwtAlgorithm] =
    Decoder.decodeString.emap(JwtAlgorithm.optionFromString(_).toRight("Invalid JWT signing algorithm"))
  implicit val jwkKeyDecoder: Decoder[JwkKey] = deriveDecoder
}
