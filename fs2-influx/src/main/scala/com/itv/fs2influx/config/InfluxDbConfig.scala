package com.itv.fs2influx.config

import org.influxdb.InfluxDB.LogLevel

final case class InfluxDbConfig(
    url: String,
    username: String,
    password: String,
    databaseName: String,
    retentionPolicy: InfluxRetentionPolicy,
    logLevel: LogLevel
)
