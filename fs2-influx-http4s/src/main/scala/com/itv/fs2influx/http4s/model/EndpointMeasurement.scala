package com.itv.fs2influx.http4s.model

import com.itv.fs2influx.model.Metric
import org.http4s.Response
import org.influxdb.dto.Point

import scala.concurrent.duration.FiniteDuration

final case class EndpointMeasurement[F[_]](endpoint: String, responseTime: FiniteDuration, response: Response[F])

object EndpointMeasurement {
  implicit def defaultMetricEncoding[F[_]]: Metric[EndpointMeasurement[F]] =
    (point: Point.Builder, field: EndpointMeasurement[F]) =>
      point
        .addField("responseTime", field.responseTime.toMillis)
        .addField("status", field.response.status.code)
        .tag("endpoint", field.endpoint)
        .tag("status", field.response.status.code.toString)
}
