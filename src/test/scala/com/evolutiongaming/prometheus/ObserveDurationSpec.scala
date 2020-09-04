package com.evolutiongaming.prometheus

import com.evolutiongaming.concurrent.CurrentThreadExecutionContext
import io.prometheus.client.Collector

class ObserveDurationSpec extends munit.FunSuite with munit.Assertions {

  private implicit val ec    = CurrentThreadExecutionContext

  def cmp(x: Double, y: Double): Boolean = Math.abs(x - y) <= 0.000000001

  def testClockPlatform(v: Long) = new ClockPlatform {
    override def nowMillis: Long = v
    override def nowNano: Long   = v
  }

  def testHasObserver(expected: Double) = new HasObserve[Unit] {

    override def observe(observer: Unit, duration: Double): Unit = {
      assert(cmp(expected, duration), clue = s"Expected duration $expected did not match calculated $duration")
    }
  }

  test("TimeToNow should correctly convert duration into double") {
    val expectedLong   = 667L
    val expectedDouble = expectedLong.toDouble / Collector.MILLISECONDS_PER_SECOND

    implicit val hasObserve: HasObserve[Unit] = testHasObserver(expectedDouble)
    implicit val clock: ClockPlatform         = testClockPlatform(expectedLong)

    val observeDuration = ObserveDuration.fromHasObserver(())
    observeDuration.timeTillNow(0L)
  }

  test("TimeToNowNano should correctly convert duration into double") {
    val expectedLong   = 730598L
    val expectedDouble = expectedLong.toDouble / Collector.NANOSECONDS_PER_SECOND

    implicit val hasObserve: HasObserve[Unit] = testHasObserver(expectedDouble)
    implicit val clock: ClockPlatform         = testClockPlatform(expectedLong)

    val observeDuration = ObserveDuration.fromHasObserver(())
    observeDuration.timeTillNowNanos(0L)
  }
}
