package com.evolutiongaming.prometheus

import io.prometheus.client.Collector

import scala.concurrent.{ExecutionContext, Future}

trait ObserveDuration[F] {

  def timeFunc[T](f: => T): T
  def timeFuncNanos[T](f: => T): T

  def timeFuture[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T]
  def timeFutureNanos[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T]

  def timeTillNow[T](start: T)(implicit numeric: Numeric[T]): Unit
  def timeTillNowNanos[T](start: T)(implicit numeric: Numeric[T]): Unit
}

object ObserveDuration {
  def apply[F](implicit observeDuration: ObserveDuration[F]): ObserveDuration[F] = observeDuration

  def fromHasObserver[F](observer: F)(implicit hasObserve: HasObserve[F], clock: ClockPlatform): ObserveDuration[F] =
    new ObserveDuration[F] {

      override def timeFunc[T](f: => T): T =
        measureFunction(f, clock.nowMillis, timeTillNow[Long])

      override def timeFuncNanos[T](f: => T): T =
        measureFunction(f, clock.nowNano, timeTillNowNanos[Long])

      private def measureFunction[A](f: => A, start: Long, measurer: Long => Unit): A =
        try f
        finally measurer(start)

      override def timeFuture[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] =
        measureFuture(f, clock.nowMillis, timeTillNow[Long])

      override def timeFutureNanos[T](f: => Future[T])(implicit ec: ExecutionContext): Future[T] =
        measureFuture(f, clock.nowNano, timeTillNowNanos[Long])

      private def measureFuture[A](
        f: => Future[A],
        start: Long,
        measurer: Long => Unit
      )(implicit ec: ExecutionContext): Future[A] = f andThen { case _ => measurer(start) }

      override def timeTillNow[T](
        start: T
      )(implicit numeric: Numeric[T]): Unit = {
        val value = duration(start, clock.nowMillis) / Collector.MILLISECONDS_PER_SECOND
        hasObserve.observe(observer, value)
      }

      override def timeTillNowNanos[T](start: T)(
        implicit numeric: Numeric[T]
      ): Unit = {
        val value = duration(start, clock.nowNano) / Collector.NANOSECONDS_PER_SECOND
        hasObserve.observe(observer, value)
      }
    }

  private def duration[A](start: A, now: Long)(implicit a: Numeric[A]): Double = {
    now.toDouble - a.toDouble(start)
  }
}
