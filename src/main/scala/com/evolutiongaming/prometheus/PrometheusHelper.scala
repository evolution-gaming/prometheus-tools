package com.evolutiongaming.prometheus

import io.prometheus.client.{Gauge, Histogram, SimpleCollector, Summary}

import scala.concurrent.Future
import scala.math.Numeric.Implicits.*

/** Main entry point for prometheus-tools goodies, mainly extension method for prometheus client classes.
  *
  * Usage:
  * {{{
  *   import com.evolutiongaming.prometheus.PrometheusHelper.*
  * }}}
  *
  * @see
  *   [[ObserveDuration]]
  */
object PrometheusHelper {
  private implicit val clock: ClockPlatform = ClockPlatform.default

  implicit val histogramObs: HasObserve[Histogram] = (histogram: Histogram, duration: Double) => histogram.observe(duration)

  implicit val histogramChildObs: HasObserve[Histogram.Child] = (child: Histogram.Child, duration: Double) => child.observe(duration)

  implicit val summaryObs: HasObserve[Summary] = (summary: Summary, duration: Double) => summary.observe(duration)

  implicit val summaryChildObs: HasObserve[Summary.Child] = (child: Summary.Child, duration: Double) => child.observe(duration)

  implicit class GaugeOps(val gauge: Gauge) extends AnyVal {

    def collect[T: Numeric](f: => T): Gauge = {
      val child = new Gauge.Child() {
        override def get(): Double = f.toDouble
      }
      gauge.setChild(child)
    }
  }

  /*
  TODO: bincompat leftover, remove in 2.x

  Without this magic method MiMa complained:
  static method TemporalOps(java.lang.Object,com.evolutiongaming.prometheus.ObserveDuration)com.evolutiongaming.prometheus.PrometheusHelper#TemporalOps
  in class com.evolutiongaming.prometheus.PrometheusHelper does not have a correspondent in current version

  The visibility also has to be public, otherwise scalac does not reliably generate static method for both 2.13 and 3
   */
  @deprecated(
    message = "referencing TemporalOps directly is deprecated, use implicit syntax",
    since = "1.1.0"
  )
  def TemporalOps[A](
    v1: A,
    v2: com.evolutiongaming.prometheus.ObserveDuration[A]
  ): com.evolutiongaming.prometheus.PrometheusHelper.TemporalOps[A] = new TemporalOps[A](v1)(v2)

  /*
  TODO: bincompat leftover, remove in 2.x

  TemporalOps wasn't needed to provide ObserveDuration syntax, should be removed - see PrometheusHelperSpec
   */
  @deprecated(
    message = "referencing TemporalOps directly is deprecated, use implicit syntax",
    since = "1.1.0"
  )
  private[prometheus] class TemporalOps[A: ObserveDuration](val a: A) {

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

  implicit class BuilderOps[C <: SimpleCollector[?], B <: SimpleCollector.Builder[
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

  implicit def observeDuration[F](observer: F)(implicit hasObserve: HasObserve[F]): ObserveDuration[F] =
    ObserveDuration.create(observer)

  implicit class RichSummaryBuilder(val summaryBuilder: Summary.Builder) extends AnyVal {

    def defaultQuantiles(): Summary.Builder = {
      summaryBuilder
        .quantile(0.9, 0.05)
        .quantile(0.99, 0.005)
    }
  }

}
