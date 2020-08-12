package com.evolutiongaming.prometheus

import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import io.prometheus.client.{Gauge, Histogram, SimpleCollector, Summary}

import scala.concurrent.Future
import scala.language.implicitConversions

object PrometheusHelper {
  private implicit val ec = CurrentThreadExecutionContext

  implicit def histogram(histogram: Histogram): HasObserve[Histogram] = new HasObserve[Histogram] {
    override def observe(duration: Double): Unit = histogram.observe(duration)
  }

  implicit def histogramChild(
    child: Histogram.Child
  ): HasObserve[Histogram.Child] = new HasObserve[Histogram.Child] {
    override def observe(duration: Double): Unit = child.observe(duration)
  }

  implicit def summary(summary: Summary): HasObserve[Summary] = new HasObserve[Summary] {
    override def observe(duration: Double): Unit = summary.observe(duration)
  }

  implicit def summaryChild(child: Summary.Child): HasObserve[Summary.Child] = new HasObserve[Summary.Child] {
    override def observe(duration: Double): Unit = child.observe(duration)
  }

  implicit class GaugeOps(val gauge: Gauge) extends AnyVal {

    def collect[T](f: => T)(implicit numeric: Numeric[T]): Gauge = {
      val child = new Gauge.Child() {
        override def get() = numeric.toDouble(f)
      }
      gauge.setChild(child)
    }
  }

  implicit class TemporalOps[A: ObserveDuration](val a: A) {

    def timeFunc[T](f: => T): T = ObserveDuration[A].timeFunc(f)

    def timeFuncNanos[T](f: => T): T = ObserveDuration[A].timeFuncNanos(f)

    def timeFuture[T](f: => Future[T]): Future[T] =
      ObserveDuration[A].timeFuture(f)

    def timeFutureNanos[T](f: => Future[T]): Future[T] =
      ObserveDuration[A].timeFutureNanos(f)

    def timeTillNow[T](start: T)(implicit numeric: Numeric[T]): Unit =
      ObserveDuration[A].timeTillNow(start)

    def timeTillNowNanos[T](start: T)(implicit numeric: Numeric[T]): Unit =
      ObserveDuration[A].timeTillNowNanos(start)
  }

  implicit class BuilderOps[C <: SimpleCollector[_], B <: SimpleCollector.Builder[
    B,
    C
  ]](val self: B)
      extends AnyVal {

    def createSimple(prefix: String, name: String): C = {
      self
        .name(s"${prefix}_$name")
        .help(s"$prefix $name")
        .create()
    }
  }

  implicit def observeDuration[F](implicit hasObserve: HasObserve[F]): ObserveDuration[F] =
    new ObserveDuration[F] {

      override def timeFunc[T](f: => T): T =
        measureFunction(f, System.currentTimeMillis(), timeTillNow[Long])

      override def timeFuncNanos[T](f: => T): T =
        measureFunction(f, System.nanoTime(), timeTillNowNanos[Long])

      private def measureFunction[A](f: => A, start: Long, measurer: Long => Unit): A =
        try f
        finally measurer(start)

      override def timeFuture[T](f: => Future[T]): Future[T] =
        measureFuture(f, System.currentTimeMillis(), timeTillNow[Long])

      override def timeFutureNanos[T](f: => Future[T]): Future[T] =
        measureFuture(f, System.nanoTime(), timeTillNowNanos[Long])

      private def measureFuture[A](
        f: => Future[A],
        start: Long,
        measurer: Long => Unit
      ): Future[A] = f andThen { case _ => measurer(start) }

      override def timeTillNow[T](
        start: T
      )(implicit numeric: Numeric[T]): Unit =
        hasObserve.observe(duration(start, System.currentTimeMillis()))

      override def timeTillNowNanos[T](start: T)(
        implicit numeric: Numeric[T]
      ): Unit = hasObserve.observe(duration(start, System.nanoTime()))
    }

  private def duration[A](start: A, now: Long)(implicit a: Numeric[A]): Double = {
    now.toDouble - a.toDouble(start)
  }
}
