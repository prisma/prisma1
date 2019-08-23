package util

import scala.concurrent.{Await, Awaitable}

trait AwaitUtils {
  import scala.concurrent.duration._

  def await[T](awaitable: Awaitable[T], seconds: Int = 5): T = {
    Await.result(awaitable, seconds.seconds)
  }

  implicit class AwaitableExtension[T](awaitable: Awaitable[T]) {
    import scala.concurrent.duration._
    def await: T = await()
    def await(seconds: Int = 50): T = {
      Await.result(awaitable, seconds.seconds)
    }
  }
}
