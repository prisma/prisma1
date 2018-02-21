package com.prisma.metrics

import com.twitter.util.{Return, Throw, Future => TwitterFuture, Promise => TwitterPromise}

import scala.concurrent.{ExecutionContext, Future => ScalaFuture, Promise => ScalaPromise}
import scala.util.Try

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

  def envVarAsInt(name: String): Option[Int] = {
    for {
      value     <- sys.env.get(name)
      converted <- Try(value.toInt).toOption
    } yield converted
  }
}
