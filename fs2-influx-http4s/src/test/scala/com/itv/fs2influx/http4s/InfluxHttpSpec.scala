package com.itv.fs2influx.http4s

import cats.effect.{IO, Resource}
import cats.implicits._
import com.itv.fs2influx.InfluxReporter
import com.itv.fs2influx.config.{BatchingConfig, InfluxDbConfig, InfluxRetentionPolicy}
import org.http4s.HttpRoutes
import org.influxdb.InfluxDB.LogLevel

import scala.concurrent.duration._

//Should compile
class InfluxHttpSpec {
  val routesOne: HttpRoutes[IO] = HttpRoutes.empty[IO]
  val routesTwo: HttpRoutes[IO] = HttpRoutes.empty[IO]

  val influxConf: InfluxDbConfig =
    InfluxDbConfig("", "", "", "", InfluxRetentionPolicy.Default, LogLevel.BASIC)
  lazy val routes: Resource[IO, HttpRoutes[IO]] =
    InfluxReporter.async[IO](influxConf, BatchingConfig(500, 10.seconds), Map.empty).map { reporter =>
      val middleware = InfluxMiddleware[IO](reporter, "test")

      val taggedRoutes = routesOne.withTag("routeOne") <+> routesTwo.withTag("routeTwo")

      middleware(taggedRoutes): HttpRoutes[IO]
    }
}
