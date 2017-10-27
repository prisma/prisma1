package cool.graph.cache

import scala.concurrent.{Await, Awaitable}

trait AwaitUtil {
  import scala.concurrent.duration._
  def await[T](awaitable: Awaitable[T]): T = Await.result(awaitable, 5.seconds)
}
