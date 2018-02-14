package com.evolutiongaming.prometheus

import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import io.prometheus.client.{Gauge, Histogram, SimpleCollector}

import scala.compat.Platform
import scala.concurrent.Future

object PrometheusHelper {
  private implicit val ec = CurrentThreadExecutionContext

  implicit class GaugeOps(val gauge: Gauge) extends AnyVal {

    def collect[T](f: => T)(implicit numeric: Numeric[T]): Gauge = {
      val child = new Gauge.Child() {
        override def get() = numeric.toDouble(f)
      }
      gauge.setChild(child)
    }
  }

  implicit class HistogramOps(val histogram: Histogram) extends AnyVal {

    def timeFunc[T](f: => T): T = {
      val start = Platform.currentTime
      try f finally histogram.timeTillNow(start)
    }

    def timeFuture[T](f: => Future[T]): Future[T] = {
      val start = Platform.currentTime
      f andThen { case _ => histogram.timeTillNow(start) }
    }

    def timeTillNow[T](start: T)(implicit numeric: Numeric[T]): Unit = {
      val now = Platform.currentTime.toDouble
      val duration = now - numeric.toDouble(start)
      histogram.observe(duration)
    }
  }

  implicit class BuilderOps[C <: SimpleCollector[_], B <: SimpleCollector.Builder[B, C]](val self: B) extends AnyVal {

    def createSimple(prefix: String, name: String): C = {
      self
        .name(s"${ prefix }_$name")
        .help(s"$prefix $name")
        .create()
    }
  }
}
