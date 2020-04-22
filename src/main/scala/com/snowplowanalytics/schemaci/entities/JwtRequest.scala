package com.snowplowanalytics.schemaci.entities

case class JwtRequest(
    clientId: String,
    clientSecret: String,
    audience: String,
    grantType: String,
    username: String,
    password: String
)
