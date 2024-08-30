package com.evolutiongaming.prometheus

import io.prometheus.client.Collector

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.Success

class ObserveDurationSpec extends munit.FunSuite with munit.Assertions {

  private val TenthOfNsInSec: Double = nsToSec(1L) / 10

  test("timeTillNowMillis should correctly convert duration into double") {
    val durMs = 667L

    implicit val hasObserve: TestHasObserve = new TestHasObserve
    implicit val clock: ClockPlatform       = testConstMillisClock(durMs)

    val observeDuration = ObserveDuration.create(())

    observeDuration.timeTillNowMillis(startMs = 0L)

    hasObserve.verifyObserved(msToSec(durMs))
  }

  test("timeTillNowNanos should correctly convert duration into double") {
    val durNs = 730598L

    implicit val hasObserve: TestHasObserve = new TestHasObserve
    implicit val clock: ClockPlatform       = testConstNanosClock(durNs)

    val observeDuration = ObserveDuration.create(())

    observeDuration.timeTillNowNanos(startNs = 0L)

    hasObserve.verifyObserved(nsToSec(durNs))
  }

  test("timeFunc should measure time with nanosecond precision") {
    val startTimeNs = 12345L
    val durNs       = 1000L

    implicit val hasObserve: TestHasObserve = new TestHasObserve
    implicit val clock: TestNanoClock       = new TestNanoClock(initialValue = startTimeNs)

    val impl = ObserveDuration.create(())

    impl.timeFunc {
      clock.advanceClock(durNs)
    }

    hasObserve.verifyObserved(nsToSec(durNs))
  }

  test("timeFuture should measure time with nanosecond precision") {
    val durNs = 1L

    implicit val hasObserve: TestHasObserve = new TestHasObserve
    implicit val clock: TestNanoClock       = new TestNanoClock()

    val observeDuration = ObserveDuration.create(())

    val finishPromise = Promise[Unit]()

    val resultF: Future[String] = observeDuration.timeFuture {
      finishPromise.future.map { _ =>
        clock.advanceClock(durNs)
        "result"
      }(ExecutionContext.parasitic)
    }

    assert(!resultF.isCompleted)
    hasObserve.verifyNothingObserved()

    finishPromise.success(())

    assert(resultF.value.contains(Success("result")))
    hasObserve.verifyObserved(nsToSec(durNs))
  }

  private def testConstMillisClock(millis: Long): ClockPlatform = new ClockPlatform {
    override def nowMillis: Long = millis

    override def nowNano: Long = ???
  }

  private def testConstNanosClock(nanos: Long): ClockPlatform = new ClockPlatform {
    override def nowMillis: Long = ???

    override def nowNano: Long = nanos
  }

  private def nsToSec(nanos: Long): Double = nanos.toDouble / Collector.NANOSECONDS_PER_SECOND

  private def msToSec(nanos: Long): Double = nanos.toDouble / Collector.MILLISECONDS_PER_SECOND

  private final class TestHasObserve extends HasObserve[Unit] {
    private val observed: mutable.ArrayBuffer[Double] = mutable.ArrayBuffer.empty

    override def observe(observer: Unit, value: Double): Unit = {
      observed += value
    }

    def verifyObserved(value: Double): Unit = {
      assert(observed.size == 1, s"only 1 value expected to be observed, got $observed")
      assertEqualsDouble(
        obtained = observed.head,
        expected = value,
        delta = TenthOfNsInSec
      )
    }

    def verifyNothingObserved(): Unit = {
      assert(observed.isEmpty, s"no values should be observed, got $observed")
    }
  }

  private final class TestNanoClock(initialValue: Long = 12345L) extends ClockPlatform {
    private var currentValue: Long = initialValue

    override def nowMillis: Long = ???

    override def nowNano: Long = currentValue

    def advanceClock(nanos: Long): Unit = {
      currentValue += nanos
    }
  }
}
