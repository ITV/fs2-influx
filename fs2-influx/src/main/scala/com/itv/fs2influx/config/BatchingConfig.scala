package com.itv.fs2influx.config

import scala.concurrent.duration.FiniteDuration

final case class BatchingConfig(batchSize: Int, flushDuration: FiniteDuration)
