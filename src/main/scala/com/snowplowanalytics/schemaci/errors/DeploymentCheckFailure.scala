package com.snowplowanalytics.schemaci.errors

import cats.data.NonEmptyList
import com.snowplowanalytics.schemaci.entities.Schema

case class DeploymentCheckFailure(schemas: NonEmptyList[Schema.Metadata])
