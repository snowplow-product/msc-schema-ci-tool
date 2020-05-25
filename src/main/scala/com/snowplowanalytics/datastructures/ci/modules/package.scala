package com.snowplowanalytics.datastructures.ci

import sttp.client.SttpBackend
import zio.{Has, Task}

package object modules {
  type SttpClient[WS[_]] = Has[SttpBackend[Task, Nothing, WS]]
  type Http              = Has[Http.Service]
  type Json              = Has[Json.Service]
  type Jwt               = Has[Jwt.Service]
  type DataStructuresApi = Has[DataStructuresApi.Service]
}
