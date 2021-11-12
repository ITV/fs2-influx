package com.itv.fs2influx.model

import org.influxdb.dto.Point

trait Metric[A] {
  def encode(point: Point.Builder, field: A): Point.Builder
}

object Metric {
  implicit val metricLong: Metric[Field[Long]] =
    (point: Point.Builder, field: Field[Long]) => point.addField(field.name, field.value)

  implicit val metricDouble: Metric[Field[Double]] =
    (point: Point.Builder, field: Field[Double]) => point.addField(field.name, field.value)

  implicit val metricString: Metric[Field[String]] =
    (point: Point.Builder, field: Field[String]) => point.addField(field.name, field.value)

  implicit val metricBoolean: Metric[Field[Boolean]] =
    (point: Point.Builder, field: Field[Boolean]) => point.addField(field.name, field.value)
}
