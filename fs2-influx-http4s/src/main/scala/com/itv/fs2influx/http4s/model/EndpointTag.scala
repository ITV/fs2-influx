package com.itv.fs2influx.http4s.model

sealed trait EndpointTag
object EndpointTag {
  final case class Present(metricName: String) extends EndpointTag
  case object None                             extends EndpointTag
}
