package com.evolutiongaming.prometheus

trait HasObserve[F] {
  def observe(observer: F, duration: Double): Unit
}

object HasObserve {
  def apply[F](implicit hasObserve: HasObserve[F]) = hasObserve
}
