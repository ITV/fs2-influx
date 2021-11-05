package com.itv.fs2influx.model

import org.influxdb.dto.Point

trait Metric[A] {
  def encode(point: Point.Builder, field: A): Point.Builder
}

object Metric {
  implicit val metricLong: Metric[Named[Long]] =
    (point: Point.Builder, field: Named[Long]) => point.addField(field.name, field.value)

  implicit val metricDouble: Metric[Named[Double]] =
    (point: Point.Builder, field: Named[Double]) => point.addField(field.name, field.value)

  implicit val metricString: Metric[Named[String]] =
    (point: Point.Builder, field: Named[String]) => point.addField(field.name, field.value)

  implicit val metricBoolean: Metric[Named[Boolean]] =
    (point: Point.Builder, field: Named[Boolean]) => point.addField(field.name, field.value)

  implicit def taggedMetric[A, P](
      implicit aField: Metric[A]
  ): Metric[Tagged[A]] =
    (point: Point.Builder, tagged: Tagged[A]) =>
      tagged.tags.foldLeft(aField.encode(point, tagged.value)) { case (p, (k, v)) => p.tag(k, v) }
}
