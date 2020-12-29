package com.snowplowanalytics.datastructures.ci

import zio.Has

package object modules {
  type Http              = Has[Http.Service]
  type Json              = Has[Json.Service]
  type Jwt               = Has[Jwt.Service]
  type DataStructuresApi = Has[DataStructuresApi.Service]
}
