package com.itv.fs2influx

import cats.effect.IO
import cats.implicits._
import com.dimafeng.testcontainers.{ContainerDef, InfluxDBContainer}
import com.dimafeng.testcontainers.munit.TestContainerForAll
import com.itv.fs2influx.config.{BatchingConfig, InfluxDbConfig, InfluxRetentionPolicy}
import munit.CatsEffectSuite
import org.influxdb.InfluxDB.LogLevel
import org.influxdb.InfluxDBFactory
import org.influxdb.dto.Query

import scala.concurrent.duration._
import scala.util.chaining.scalaUtilChainingOps

class AsyncInfluxReporterTests extends CatsEffectSuite with TestContainerForAll {
  val containerDef: ContainerDef = InfluxDBContainer.Def()

  def makeConfig(container: InfluxDBContainer) = InfluxDbConfig(
    container.url,
    container.admin,
    container.adminPassword,
    container.database,
    InfluxRetentionPolicy.Custom("foo"),
    LogLevel.NONE
  )

  override def afterContainersStart(containers: Containers): Unit =
    containers match {
      case c: InfluxDBContainer =>
        InfluxDBFactory
          .connect(c.url, c.admin, c.adminPassword)
          .query(new Query("CREATE RETENTION POLICY foo ON test DURATION INF REPLICATION 1", "test", true))
          .pipe(_ => ())
      case _ => ()
    }

  test("should write to influx") {
    withContainers { case container: InfluxDBContainer =>
      val config = makeConfig(container)
      InfluxReporter
        .async[IO](
          config,
          BatchingConfig(
            10,
            500.millis
          ),
          Map("test" -> "tag")
        )
        .use { influx =>
          val length = 20
          val points =
            List.tabulate(length)(i => influx.point(s"test_measure").addField(s"field", i))
          for {
            _ <- points.traverse_(influx.write)
            _ <- IO.sleep(1.seconds) // just to wait for everything to be taken off the internal queue and written
            queryRes <- IO.delay(influx.underlying.query(new Query("SELECT * FROM test_measure", "test")))
            valuesWritten = queryRes.getResults.get(0).getSeries.get(0).getValues
          } yield assertEquals(valuesWritten.size, length)
        }
    }
  }

  test("should write remaining point to influx on shutdown") {
    withContainers { case container: InfluxDBContainer =>
      val config = makeConfig(container)
      val length = 20

      InfluxReporter
        .async[IO](
          config,
          BatchingConfig(
            10,
            500.millis
          ),
          Map("test" -> "tag")
        )
        .allocated
        .flatMap { case (influx, shutdown) =>
          val points = List.tabulate(length)(i => influx.point(s"test_measure_2").addField(s"field", i))
          for {
            _ <- points.traverse_(influx.write)
            _ <- shutdown
            queryRes <- IO.delay(
              InfluxDBFactory
                .connect(container.url, container.username, container.password)
                .query(new Query("SELECT * FROM test_measure_2", "test"))
            )
            valuesWritten = queryRes.getResults.get(0).getSeries.get(0).getValues
          } yield assertEquals(valuesWritten.size, length)
        }
    }
  }
}
