package com.evolutiongaming.prometheus

import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import io.prometheus.client.{Gauge, Histogram, SimpleCollector, Summary}

import scala.concurrent.Future

object PrometheusHelper {
  private implicit val ec    = CurrentThreadExecutionContext
  private implicit val clock = ClockPlatform.default

  implicit val histogramObs: HasObserve[Histogram] = (histogram: Histogram, duration: Double) => histogram.observe(duration)

  implicit val histogramChildObs: HasObserve[Histogram.Child] = (child: Histogram.Child, duration: Double) => child.observe(duration)

  implicit val summaryObs: HasObserve[Summary] = (summary: Summary, duration: Double) => summary.observe(duration)

  implicit val summaryChildObs: HasObserve[Summary.Child] = (child: Summary.Child, duration: Double) => child.observe(duration)

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

  implicit def observeDuration[F](observer: F)(implicit hasObserve: HasObserve[F]): ObserveDuration[F] = ObserveDuration.fromHasObserver(observer)

  implicit class RichSummaryBuilder(val summaryBuilder: Summary.Builder) extends AnyVal {

    def defaultQuantiles(): Summary.Builder = {
      summaryBuilder
        .quantile(0.9, 0.05)
        .quantile(0.99, 0.005)
    }
  }

}
