package com.prisma.tracing

import scala.concurrent.{ExecutionContext, Future}

trait Tracing {

  def timeFuture[T](name: String)(f: => Future[T])(implicit ec: ExecutionContext): Future[T] = {
    val startTime = java.lang.System.nanoTime()
    val result    = f

    result.onComplete { _ =>
      println(name + ": " + ((java.lang.System.nanoTime() - startTime) / 1000))
    }

    result
  }

  def time[T](name: String)(f: => T): T = {
    val startTime = java.lang.System.nanoTime()
    val res       = f

    println(name + ": " + ((java.lang.System.nanoTime() - startTime) / 1000))
    res
  }
}
