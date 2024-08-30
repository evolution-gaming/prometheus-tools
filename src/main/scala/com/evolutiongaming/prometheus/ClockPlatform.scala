package com.evolutiongaming.prometheus

/** Exposes platform time measuring capabilities needed for metrics
  */
trait ClockPlatform {

  /** @see
    *   [[System#currentTimeMillis()]]
    */
  def nowMillis: Long

  /** @see
    *   [[System#nanoTime()]]
    */
  def nowNano: Long
}

object ClockPlatform {

  /** Global singleton instance for default [[ClockPlatform]] for JVM
    */
  val default: ClockPlatform = new Default

  /** Default [[ClockPlatform]] impl for JVM - use [[ClockPlatform.default]] global singleton instance!
    */
  class Default extends ClockPlatform {
    override def nowMillis: Long = System.currentTimeMillis()
    override def nowNano: Long   = System.nanoTime()
  }

}
