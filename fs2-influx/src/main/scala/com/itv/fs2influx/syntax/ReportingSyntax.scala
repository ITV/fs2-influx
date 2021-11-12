package com.itv.fs2influx.syntax

import cats.{ApplicativeError, FlatMap, Functor}
import cats.implicits._
import cats.effect.implicits._
import cats.effect.Clock
import com.itv.fs2influx.InfluxReporter
import com.itv.fs2influx.model._

import scala.concurrent.duration.FiniteDuration

trait ReportingSyntax extends TagSyntax {
  implicit class MetricOps[A](value: A) {
    def report[F[_]](
        measurement: String
    )(implicit metric: Metric[A], reporter: InfluxReporter[F], F: Functor[F]): F[A] =
      reporter.write(measurement, value).as(value)
  }

  implicit class MetricOpsF[F[_], A](value: F[A]) {
    def reportWithDuration(
        measurement: String
    )(
        implicit metric: Metric[(FiniteDuration, A)],
        reporter: InfluxReporter[F],
        clock: Clock[F],
        F: FlatMap[F]
    ): F[A] =
      value.timed
        .flatTap(reporter.write(measurement, _))
        .map(_._2)

    def reportDuration(
        measurement: String
    )(
        implicit metric: Metric[(FiniteDuration, Unit)],
        reporter: InfluxReporter[F],
        clock: Clock[F],
        F: FlatMap[F]
    ): F[A] =
      value.timed
        .flatTap(_.void.report(measurement))
        .map(_._2)

    def reportDurationTagged(
        measurement: String,
        tag: Named[String]
    )(
        implicit metric: Metric[Tagged[(FiniteDuration, Unit)]],
        reporter: InfluxReporter[F],
        clock: Clock[F],
        F: FlatMap[F]
    ): F[A] =
      value.timed
        .flatTap(_.void.tagged(tag).report(measurement))
        .map(_._2)

    def reportWithDurationTagged(
        measurement: String,
        tag: Named[String]
    )(
        implicit metric: Metric[Tagged[(FiniteDuration, A)]],
        reporter: InfluxReporter[F],
        clock: Clock[F],
        F: FlatMap[F]
    ): F[A] =
      value.timed
        .taggedAs(tag)
        .flatTap(reporter.write(measurement, _))
        .map(_.value._2)

    def reportError[E](
        measurement: String,
        shouldReport: E => Boolean
    )(
        implicit metric: Metric[E],
        reporter: InfluxReporter[F],
        F: ApplicativeError[F, E]
    ): F[A] =
      value.onError {
        case e if shouldReport(e) =>
          e.report(measurement).void
      }
  }
}
