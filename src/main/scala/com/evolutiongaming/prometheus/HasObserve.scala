package com.evolutiongaming.prometheus

/** A type-class abstracting over prometheus client histogram-like classes providing "observe a Double value" method.
  *
  * Check [[PrometheusHelper]] for available implicit instances.
  */
trait HasObserve[F] {

  /** Observe a new sample value on a histogram-like metric type
    */
  def observe(observer: F, value: Double): Unit
}

object HasObserve {
  def apply[F](implicit hasObserve: HasObserve[F]): HasObserve[F] = hasObserve
}
