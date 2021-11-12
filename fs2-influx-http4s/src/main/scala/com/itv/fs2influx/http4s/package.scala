package com.itv.fs2influx

import cats.data.OptionT
import cats.{Functor, ~>}
import com.itv.fs2influx.http4s.model.EndpointTag
import org.http4s.{Http, HttpRoutes}

package object http4s {
  implicit class HttpOps[F[_]](val http: HttpRoutes[F]) extends AnyVal {
    def withTag(endpointTag: String)(implicit F: Functor[F]): Http[MetricTagged[F, *], F] =
      http.mapK(new (OptionT[F, *] ~> MetricTagged[OptionT[F, *], *]) {
        def apply[A](fa: OptionT[F, A]): MetricTagged[OptionT[F, *], A] =
          MetricTagged[OptionT[F, *], A](fa.map(a => (EndpointTag.Present(endpointTag), a)))
      })

    def withoutTag(implicit F: Functor[F]): Http[MetricTagged[F, *], F] =
      http.mapK(new (OptionT[F, *] ~> MetricTagged[OptionT[F, *], *]) {
        def apply[A](fa: OptionT[F, A]): MetricTagged[OptionT[F, *], A] =
          MetricTagged[OptionT[F, *], A](fa.map(a => (EndpointTag.None, a)))
      })
  }
}
