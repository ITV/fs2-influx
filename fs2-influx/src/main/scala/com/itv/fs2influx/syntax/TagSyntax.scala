package com.itv.fs2influx.syntax

import cats.Functor
import cats.implicits._
import com.itv.fs2influx.model.{Named, Tagged}

trait TagSyntax {
  implicit class TagSyntax[A](val value: A) {
    def tagged(key: String, tagValue: String): Tagged[A] = Tagged(value, Map(key -> tagValue))

    def tagged(named: Named[String]): Tagged[A] = tagged(named.name, named.value)
  }

  implicit class TagFSyntax[F[_], A](val value: F[A]) {
    def taggedAs(named: Named[String])(implicit F: Functor[F]): F[Tagged[A]] =
      value.map(_.tagged(named))
  }
}
