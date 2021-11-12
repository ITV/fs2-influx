package com.itv.fs2influx.model

import org.influxdb.dto.Point

import scala.concurrent.duration.FiniteDuration

trait Metric[A] {
  def encode(point: Point.Builder, field: A): Point.Builder
}

object Metric {
  def apply[A](implicit ev: Metric[A]): Metric[A] = ev

  implicit val metricLong: Metric[Named[Long]] =
    (point: Point.Builder, field: Named[Long]) => point.addField(field.name, field.value)

  implicit val metricDouble: Metric[Named[Double]] =
    (point: Point.Builder, field: Named[Double]) => point.addField(field.name, field.value)

  implicit val metricString: Metric[Named[String]] =
    (point: Point.Builder, field: Named[String]) => point.addField(field.name, field.value)

  implicit val metricBoolean: Metric[Named[Boolean]] =
    (point: Point.Builder, field: Named[Boolean]) => point.addField(field.name, field.value)

  implicit def timedMetric[A: Metric]: Metric[(FiniteDuration, A)] =
    (point: Point.Builder, field: (FiniteDuration, A)) =>
      Metric[A].encode(point, field._2).addField("elapsed", field._1.toMillis)
}
