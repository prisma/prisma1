package cool.graph.metrics

import com.twitter.util.{Return, Throw, Future => TwitterFuture, Promise => TwitterPromise}
import scala.concurrent.{ExecutionContext, Future => ScalaFuture, Promise => ScalaPromise}

object Utils {

  implicit class RichTwitterFuture[A](val tf: TwitterFuture[A]) extends AnyVal {
    def asScala(implicit e: ExecutionContext): ScalaFuture[A] = {
      val promise: ScalaPromise[A] = ScalaPromise()
      tf.respond {
        case Return(value)    => promise.success(value)
        case Throw(exception) => promise.failure(exception)
      }
      promise.future
    }
  }
}
