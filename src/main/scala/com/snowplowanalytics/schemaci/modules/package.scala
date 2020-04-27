package com.snowplowanalytics.schemaci

import zio.Has

package object modules {
  type Http      = Has[Http.Service]
  type Json      = Has[Json.Service]
  type Jwt       = Has[Jwt.Service]
  type SchemaApi = Has[SchemaApi.Service]
}
