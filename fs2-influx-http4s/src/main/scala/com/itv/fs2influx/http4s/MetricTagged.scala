package com.itv.fs2influx.http4s

import cats.SemigroupK
import cats.implicits._
import com.itv.fs2influx.http4s.model.EndpointTag

final case class MetricTagged[F[_], A](value: F[(EndpointTag, A)])

object MetricTagged {
  implicit def semigroupK[F[_]: SemigroupK]: SemigroupK[MetricTagged[F, *]] = new SemigroupK[MetricTagged[F, *]] {
    def combineK[A](x: MetricTagged[F, A], y: MetricTagged[F, A]): MetricTagged[F, A] = MetricTagged(
      x.value.combineK(y.value)
    )
  }
}
