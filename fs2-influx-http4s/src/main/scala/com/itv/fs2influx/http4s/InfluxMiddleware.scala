package com.itv.fs2influx.http4s

import cats.Monad
import cats.data.{Kleisli, OptionT}
import cats.effect.implicits._
import cats.effect.kernel.Clock
import cats.implicits._
import com.itv.fs2influx.InfluxReporter
import com.itv.fs2influx.http4s.model._
import com.itv.fs2influx.model.Metric
import org.http4s.{Http, HttpApp, HttpRoutes, Request, Response}

object InfluxMiddleware {
  def apply[F[_]](reporter: InfluxReporter[F], prefix: String): InfluxMiddleware[F] =
    new InfluxMiddleware[F](reporter, prefix) {}
}

sealed abstract class InfluxMiddleware[F[_]](reporter: InfluxReporter[F], prefix: String) {
  def apply(
      taggedRoutes: Http[MetricTagged[F, *], F]
  )(implicit clock: Clock[F], F: Monad[F], encoder: Metric[EndpointMeasurement[F]]): HttpRoutes[F] =
    HttpRoutes { req =>
      val response =
        measureResponse(taggedRoutes, req)
      OptionT.some(response)
    }

  def app(
      taggedRoutes: Http[MetricTagged[F, *], F]
  )(implicit clock: Clock[F], F: Monad[F], encoder: Metric[EndpointMeasurement[F]]): HttpApp[F] =
    Kleisli { req =>
      measureResponse(taggedRoutes, req)
    }

  private def measureResponse(
      taggedRoutes: Http[MetricTagged[F, *], F],
      req: Request[F]
  )(implicit clock: Clock[F], F: Monad[F], encoder: Metric[EndpointMeasurement[F]]): F[Response[F]] =
    taggedRoutes
      .run(req)
      .value
      .timed
      .flatMap { case (time, (tag, response)) =>
        tag match {
          case EndpointTag.Present(metricName) =>
            reporter
              .write(s"$prefix.response", EndpointMeasurement[F](metricName, time, response))
              .as(response)
          case EndpointTag.None => F.pure(response)
        }
      }
}
