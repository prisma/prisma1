package com.prisma.akkautil.throttler

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import com.prisma.akkautil.specs2.{AcceptanceSpecification, AkkaTestKitSpecs2Context}
import com.prisma.akkautil.throttler.Throttler.{ThrottleBufferFullException, ThrottleCallTimeoutException}

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{Await, Awaitable, Future}

class ThrottlerSpec extends AcceptanceSpecification {
  def is = s2"""
    The Throttler must
      make the call if throttle rate is not reached $rate_not_reached
      make the call later if the throttle rate is reached $rate_reached
      make the call and result in a ThrottleBufferFullException if the call buffer is full $buffer_full
  """

  // make the call and result in a ThrottleCallTimeoutException if the call takes too long $timeout_hit

  def rate_not_reached = new AkkaTestKitSpecs2Context {
    val throttler    = testThrottler()
    var callExecuted = false

    val result = throttler
      .throttled("group") { () =>
        callExecuted = true
        Future.successful("the-result")
      }
      .await

    result.result mustEqual "the-result"
    callExecuted must beTrue
  }

  def rate_reached = new AkkaTestKitSpecs2Context {
    for (_ <- 1 to 10) {
      val throttler = testThrottler(ratePer100ms = 1)
      val group     = "group"
      // make one call; rate is reached now
      throttler.throttled(group) { () =>
        Future.successful("the-result")
      }

      // second call must be throttled and should take around 1 second
      val begin = System.currentTimeMillis
      throttler
        .throttled(group) { () =>
          Future.successful("the-result")
        }
        .await
      val end = System.currentTimeMillis
      (end - begin) must be_>(100L)
    }
  }

  def timeout_hit = new AkkaTestKitSpecs2Context {
    for (_ <- 1 to 10) {
      val throttler = testThrottler(timeoutInMillis = 100)
      val group     = "group"

      throttler
        .throttled(group) { () =>
          Future {
            Thread.sleep(125)
          }(system.dispatcher)
        }
        .await must throwA[ThrottleCallTimeoutException]
    }
  }

  def buffer_full = new AkkaTestKitSpecs2Context {
    for (_ <- 1 to 10) {
      val throttler = testThrottler(ratePer100ms = 1, bufferSize = 1)
      val group     = "group"

      // make one call; rate is reached now
      throttler
        .throttled(group) { () =>
          Future.successful("the-result")
        }
        .await // waits to make sure in flight count is 0

      // make more calls; buffer is full now
      throttler.throttled(group) { () =>
        Future.successful("the-result")
      }

      // next call must result in exception
      throttler
        .throttled(group) { () =>
          Future.successful("the-result")
        }
        .await must throwA[ThrottleBufferFullException]
    }
  }

  def testThrottler(timeoutInMillis: Int = 10000, ratePer100ms: Int = 10, bufferSize: Int = 100)(implicit as: ActorSystem): Throttler[String] = {
    Throttler[String](
      groupBy = identity,
      amount = ratePer100ms,
      per = FiniteDuration(100, TimeUnit.MILLISECONDS),
      timeout = akka.util.Timeout(timeoutInMillis, TimeUnit.MILLISECONDS),
      maxCallsInFlight = bufferSize
    )
  }

  implicit class AwaitableExtension[T](awaitable: Awaitable[T]) {
    import scala.concurrent.duration._
    def await: T = {
      Await.result(awaitable, 5.seconds)
    }
  }

}
