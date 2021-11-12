package com.itv.fs2influx.model

final case class Field[A](name: String, value: A)
