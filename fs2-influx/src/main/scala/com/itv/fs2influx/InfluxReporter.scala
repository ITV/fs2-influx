package com.itv.fs2influx

import cats.Functor
import cats.effect.std.Random
import cats.effect._
import cats.implicits._
import com.itv.fs2influx.config.{BatchingConfig, InfluxDbConfig}
import com.itv.fs2influx.model.Metric
import fs2.Stream
import fs2.concurrent.Channel
import org.influxdb.dto.{BatchPoints, Point}
import org.influxdb.{InfluxDB, InfluxDBFactory}

import java.util.concurrent.TimeUnit
import scala.concurrent.duration._
import scala.jdk.CollectionConverters._
import scala.util.control.NonFatal

trait InfluxReporter[F[_]] {
  def write(value: Point.Builder): F[Unit]

  def write[A](name: String, value: A)(implicit metric: Metric[A]): F[Unit] =
    write(metric.encode(point(name), value))

  def point(measurement: String): Point.Builder
}

object InfluxReporter {
  def sync[F[_]: Sync](
      config: InfluxDbConfig,
      tags: Map[String, String]
  ): Resource[F, BlockingInfluxReporter[F]] = {
    val underlying: InfluxDB =
      InfluxDBFactory
        .connect(config.url, config.username, config.password)
        .setDatabase(config.databaseName)
        .setRetentionPolicy(config.retentionPolicy.value)
        .setLogLevel(config.logLevel)

    val influx =
      Random.javaUtilRandom[F](new java.util.Random()).map { implicit r =>
        new BlockingInfluxReporter[F](underlying, tags) {}
      }

    Resource.make(influx) { reporter =>
      Sync[F].delay(reporter.underlying.flush()) >> Sync[F].delay(reporter.underlying.close())
    }
  }

  def async[F[_]: Async](
      config: InfluxDbConfig,
      tags: Map[String, String]
  ): Resource[F, AsyncInfluxReporter[F]] = {
    val underlying: InfluxDB =
      InfluxDBFactory
        .connect(config.url, config.username, config.password)
        .setDatabase(config.databaseName)
        .setRetentionPolicy(config.retentionPolicy.value)
        .setLogLevel(config.logLevel)
        .enableBatch()

    val influx = Channel.unbounded[F, Point].flatMap { channel =>
      Random.javaUtilRandom[F](new java.util.Random()).map { implicit r =>
        new AsyncInfluxReporter[F](underlying, channel, config, tags) {}
      }
    }

    Resource.eval(influx).flatTap(_.initialiseWriter)
  }

  // timestamp is a primary key in influx, so this ensures that any two points get written separately
  private def addRandomNanos[F[_]: Functor: Random](now: FiniteDuration): F[Long] =
    Random[F].nextLongBounded(1000000).map(_ + (now.toMillis * 1000000))

  sealed abstract class BlockingInfluxReporter[F[_]: Sync: Random](
      val underlying: InfluxDB,
      tags: Map[String, String]
  ) extends TaggedInfluxReporter[F](tags) {
    def write(builder: Point.Builder): F[Unit] =
      Clock[F].realTime.flatMap { now =>
        addRandomNanos[F](now).flatMap { timeInNanos =>
          Sync[F].blocking {
            underlying.write(
              builder
                .time(timeInNanos, TimeUnit.NANOSECONDS)
                .build()
            )
          }
        }.void
      }
  }

  sealed abstract class AsyncInfluxReporter[F[_]: Async: Random](
      val underlying: InfluxDB,
      channel: Channel[F, Point],
      config: InfluxDbConfig,
      tags: Map[String, String]
  ) extends TaggedInfluxReporter[F](tags) {

    val batching: BatchingConfig.Enabled = config.batching match {
      case BatchingConfig.Disabled   => BatchingConfig.defaults
      case b: BatchingConfig.Enabled => b
    }

    override def write(builder: Point.Builder): F[Unit] =
      Clock[F].realTime.flatMap { now =>
        addRandomNanos[F](now).flatMap { timeInNanos =>
          channel.send(
            builder
              .time(timeInNanos, TimeUnit.NANOSECONDS)
              .build()
          )
        }.void
      }

    protected[fs2influx] def initialiseWriter: Resource[F, F[Outcome[F, Throwable, Unit]]] =
      Resource
        .make {
          Spawn[F].start {
            loop(
              channel.stream
                .groupWithin(batching.batchSize, batching.flushDuration)
                .evalMap { points =>
                  val batchPoints = BatchPoints
                    .builder()
                    .points(points.toList.asJava)
                    .build()
                  Sync[F].delay(underlying.writeWithRetry(batchPoints))
                },
              1
            ).compile.drain
          }
        }(shutdown)
        .map(_.join)

    private def shutdown(fiber: Fiber[F, Throwable, Unit]): F[Unit] =
      channel.close.void *>                          // Stream should consume all remaining queued elements
        Temporal[F].sleep(batching.flushDuration) *> // wait for stream to flush last batch
        Sync[F].delay(underlying.flush()) *>         // influx should write remaining to db
        Sync[F].delay(underlying.close()) *>
        fiber.cancel

    // stolen from https://github.com/profunktor/fs2-rabbit/blob/master/core/src/main/scala/dev/profunktor/fs2rabbit/resiliency/ResilientStream.scala
    private def loop(program: Stream[F, Unit], count: Int): Stream[F, Unit] = program.handleErrorWith {
      case NonFatal(_) => Stream.sleep[F]((2 * count).seconds) >> loop(program, count + 1)
      case _           => ???
    }
  }

  sealed abstract class TaggedInfluxReporter[F[_]](tags: Map[String, String]) extends InfluxReporter[F] {
    override def point(measurement: String): Point.Builder = Point.measurement(measurement).tag(tags.asJava)
  }
}
