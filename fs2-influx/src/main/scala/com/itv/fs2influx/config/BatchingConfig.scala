package com.itv.fs2influx.config

import scala.concurrent.duration.{DurationInt, FiniteDuration}

sealed trait BatchingConfig

object BatchingConfig {
  val defaults: Enabled = Enabled(500, 5.seconds)

  case object Disabled                                                    extends BatchingConfig
  final case class Enabled(batchSize: Int, flushDuration: FiniteDuration) extends BatchingConfig
}
