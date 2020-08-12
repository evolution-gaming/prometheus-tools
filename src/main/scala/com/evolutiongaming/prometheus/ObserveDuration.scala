package com.evolutiongaming.prometheus

import scala.concurrent.Future

trait ObserveDuration[F] {

  def timeFunc[T](f: => T): T
  def timeFuncNanos[T](f: => T): T

  def timeFuture[T](f: => Future[T]): Future[T]
  def timeFutureNanos[T](f: => Future[T]): Future[T]

  def timeTillNow[T](start: T)(implicit numeric: Numeric[T]): Unit
  def timeTillNowNanos[T](start: T)(implicit numeric: Numeric[T]): Unit
}

object ObserveDuration {
  def apply[F](implicit observeDuration: ObserveDuration[F]): ObserveDuration[F] = observeDuration
}
