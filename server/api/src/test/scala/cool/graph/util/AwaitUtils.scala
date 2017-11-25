package cool.graph.util

import scala.concurrent.{Await, Awaitable}

trait AwaitUtils {
  import scala.concurrent.duration._

  def await[T](awaitable: Awaitable[T], seconds: Int = 5): T = {
    Await.result(awaitable, seconds.seconds)
  }

  implicit class AwaitableExtension[T](awaitable: Awaitable[T]) {
    import scala.concurrent.duration._
    def await(seconds: Int = 5): T = {
      Await.result(awaitable, seconds.seconds)
    }
  }
}
