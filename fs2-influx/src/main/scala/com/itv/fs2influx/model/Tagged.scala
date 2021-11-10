package com.itv.fs2influx.model

final case class Tagged[A](value: A, tags: Map[String, String]) {
  def tagged(key: String, tagValue: String): Tagged[A] =
    Tagged(value, tags.updated(key, tagValue))
}
