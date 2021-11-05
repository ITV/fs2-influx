package com.itv.fs2influx.config

sealed trait InfluxRetentionPolicy {
  val value: String
}

object InfluxRetentionPolicy {
  case object Default extends InfluxRetentionPolicy {
    val value: String = "default"
  }

  case class Days(days: Int) extends InfluxRetentionPolicy {
    val value: String = s"${days}d"
  }

  case class Custom(value: String) extends InfluxRetentionPolicy

  val fromString: String => InfluxRetentionPolicy = {
    case Default.value => Default
    case custom        => Custom(custom)
  }
}
