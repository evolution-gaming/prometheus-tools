package com.evolutiongaming.prometheus

import io.prometheus.client.{Collector, SimpleTimer}

import scala.annotation.{nowarn, unused}
import scala.concurrent.{ExecutionContext, Future}
import scala.math.Numeric.Implicits.*
import scala.util.Try

/** Time duration measurement syntax for [[HasObserve]]-kind metrics from the prometheus client, i.e., Summary, Histogram.
  *
  * Here time is always reported in seconds, which means your prometheus metric name should end in `_seconds`.
  *
  * The class is not supposed to be used directly but as an implicit syntax provided by [[PrometheusHelper]].
  */
sealed trait ObserveDuration[F] {

  /** Measures evaluation time of a block in seconds with nano-time precision
    */
  def timeFunc[T](f: => T): T

  /** Measures evaluation time of a block in seconds with nano-time precision
    *
    * @deprecated
    *   since 1.1.0 timeFunc has been changed to use nano-time precision, this method is obsolete and will be removed
    */
  @deprecated(
    message = "use timeFunc instead - it has nano-time precision now",
    since = "1.1.0"
  )
  def timeFuncNanos[T](f: => T): T = timeFunc(f)

  /** Measures evaluation time of an asynchronous block in seconds with nano-time precision
    */
  def timeFuture[T](f: => Future[T]): Future[T]

  /** Measures evaluation time of an asynchronous block in seconds with nano-time precision
    *
    * @deprecated
    *   since 1.1.0 timeFuture has been changed to use nano-time precision, this method is obsolete and will be removed
    */
  @deprecated(
    message = "use timeFuture instead - it has nano-time precision now",
    since = "1.1.0"
  )
  def timeFutureNanos[T](f: => Future[T]): Future[T] = timeFuture(f)

  /** Measures in seconds the time spent since the provided start time obtained using [[ClockPlatform.nowMillis]]
    *
    * @param start
    *   start time from a millisecond-precision clock
    * @deprecated
    *   since 1.1.0, use timeTillNowMillis(: Long) with a primitive arg type and explicit precision name suffix
    */
  @deprecated(
    message = "use timeTillNowMillis(: Long) with a primitive arg type and explicit precision name suffix",
    since = "1.1.0"
  )
  def timeTillNow[T](start: T)(implicit numeric: Numeric[T]): Unit

  /** Measures in seconds the time spent since the provided start time obtained using [[ClockPlatform.nowNano]]
    *
    * @param start
    *   start time from a nanosecond-precision clock
    * @deprecated
    *   since 1.1.0, use timeTillNowNanos(: Long) with a primitive arg type
    */
  @deprecated(
    message = "use timeTillNowNanos(: Long) with a primitive arg type",
    since = "1.1.0"
  )
  def timeTillNowNanos[T](start: T)(implicit numeric: Numeric[T]): Unit

  /** Measures in seconds the time spent since the provided start time obtained using [[ClockPlatform.nowMillis]]
    */
  def timeTillNowMillis(startMs: Long): Unit = {
    // default impl for a new method of a trait - added for keeping binary compatibility
    // TODO: bincompat leftover, remove in 2.x

    timeTillNow[Long](startMs): @nowarn("cat=deprecation")
  }

  /** Measures in seconds the time spent since the provided start time obtained using [[ClockPlatform.nowNano]]
    */
  def timeTillNowNanos(startNs: Long): Unit = {
    // default impl for a new method of a trait - added for keeping binary compatibility
    // TODO: bincompat leftover, remove in 2.x

    timeTillNowNanos[Long](startNs): @nowarn("cat=deprecation")
  }
}

object ObserveDuration {
  def apply[F](implicit observeDuration: ObserveDuration[F]): ObserveDuration[F] = observeDuration

  // TODO: bincompat leftover, remove in 2.x
  @deprecated(
    message = "use create",
    since = "1.1.0"
  )
  def fromHasObserver[F](
    observer: F
  )(implicit
    hasObserve: HasObserve[F],
    clock: ClockPlatform,
    @unused ec: ExecutionContext
  ): ObserveDuration[F] = new ObserveDurationImpl[F](observer)

  /** Creates [[ObserveDuration]] implementation instance
    *
    * @param observer
    *   metric instance which has [[HasObserve]]
    * @param hasObserve
    *   [[HasObserve]] instance for the metric
    * @param clock
    *   [[ClockPlatform]] to use for time measurement
    * @tparam F
    *   metric type
    */
  def create[F](
    observer: F
  )(implicit
    hasObserve: HasObserve[F],
    clock: ClockPlatform
  ): ObserveDuration[F] = new ObserveDurationImpl[F](observer)

  private final class ObserveDurationImpl[F](
    observer: F
  )(implicit
    hasObserve: HasObserve[F],
    clock: ClockPlatform
  ) extends ObserveDuration[F] {

    override def timeFunc[T](f: => T): T = {
      val startNs = clock.nowNano
      try {
        f
      } finally {
        timeTillNowNanos(startNs)
      }
    }

    override def timeFuture[T](f: => Future[T]): Future[T] = {
      val startNs = clock.nowNano
      Future
        .fromTry(Try {
          f
        })
        .flatten
        .andThen { case _ =>
          timeTillNowNanos(startNs)
        }(ExecutionContext.parasitic)
    }

    override def timeTillNow[T](
      start: T
    )(implicit numeric: Numeric[T]): Unit = {
      val value = duration(start, clock.nowMillis) / Collector.MILLISECONDS_PER_SECOND
      hasObserve.observe(observer, value)
    }

    override def timeTillNowNanos[T](start: T)(implicit
      numeric: Numeric[T]
    ): Unit = {
      val value = duration(start, clock.nowNano) / Collector.NANOSECONDS_PER_SECOND
      hasObserve.observe(observer, value)
    }

    override def timeTillNowMillis(startMs: Long): Unit = {
      val endMs          = clock.nowMillis
      val elapsedSeconds = (endMs - startMs).toDouble / Collector.MILLISECONDS_PER_SECOND
      hasObserve.observe(observer, elapsedSeconds)
    }

    override def timeTillNowNanos(startNs: Long): Unit = {
      val endNs = clock.nowNano
      hasObserve.observe(observer, SimpleTimer.elapsedSecondsFromNanos(startNs, endNs))
    }
  }

  private def duration[A: Numeric](start: A, now: Long): Double = {
    now.toDouble - start.toDouble
  }
}
