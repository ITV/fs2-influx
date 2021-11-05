package com.itv.fs2influx.model

final case class Named[A](name: String, value: A)
