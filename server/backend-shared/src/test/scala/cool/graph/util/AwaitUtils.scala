package cool.graph.util

import scala.concurrent.{Await, Awaitable}

object AwaitUtils {
  def await[T](awaitable: Awaitable[T]): T = {
    import scala.concurrent.duration._
    Await.result(awaitable, 5.seconds)
  }

  implicit class AwaitableExtension[T](awaitable: Awaitable[T]) {
    import scala.concurrent.duration._
    def await: T = {
      Await.result(awaitable, 5.seconds)
    }
  }
}
