package com.evolutiongaming.prometheus

import scala.annotation.unused
import scala.concurrent.Future

class PrometheusHelperSpec extends munit.FunSuite with munit.Assertions {
  test("Provide ObserveDuration syntax for concrete HasObserve instances") {
    /*
    Smoke test verifying that PrometheusHelper implicits machinery still provides required ObserveDuration syntax.
    Nothing is executed here, but the code has to compile.
     */

    import com.evolutiongaming.prometheus.PrometheusHelper.*

    @unused
    def fun(s: io.prometheus.client.Summary): Unit = {
      @unused
      val syncRes: Double = s.timeFunc {
        Thread.sleep(1000L)
        1d
      }

      @unused
      val futRes: Future[Int] = s.timeFuture {
        Future.successful(1)
      }

      ()
    }
  }
}
