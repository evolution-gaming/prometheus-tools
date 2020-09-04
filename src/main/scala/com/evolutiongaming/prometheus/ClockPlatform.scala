package com.evolutiongaming.prometheus

trait ClockPlatform {
  def nowMillis: Long
  def nowNano: Long
}

object ClockPlatform {

  val default: ClockPlatform = new Default

  class Default extends ClockPlatform {
    override def nowMillis: Long = System.currentTimeMillis()
    override def nowNano: Long   = System.nanoTime()
  }

}
