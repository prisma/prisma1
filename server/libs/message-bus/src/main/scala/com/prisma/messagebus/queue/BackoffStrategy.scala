package com.prisma.messagebus.queue

import akka.actor.ActorSystem
import akka.pattern.after
import com.prisma.akkautil.SingleThreadedActorSystem

import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

object BackoffStrategy {
  import scala.concurrent.ExecutionContext.Implicits.global
//  val system = SingleThreadedActorSystem("backoff")

  def backoffDurationFor(currentTry: Int, strategy: BackoffStrategy): FiniteDuration = {
    strategy match {
      case ConstantBackoff(d) => d
      case LinearBackoff(d)   => d * currentTry
    }
  }

  def backoff(duration: FiniteDuration)(implicit sys: ActorSystem): Future[Unit] = after(duration, sys.scheduler)(Future.successful(Unit))
}

sealed trait BackoffStrategy {
  val duration: FiniteDuration
}

/**
  * A constant backoff always stays at the specified duration, for each try.
  * E.g. a constant backoff of 5 seconds backs off for 5 seconds after the 1st and 2nd try, totalling to 10 seconds over 2 tries.
  */
case class ConstantBackoff(duration: FiniteDuration) extends BackoffStrategy

/**
  * A linear backoff increases the backoff duration linearly over the number of tries.
  * E.g. the backoff of 5 seconds is 5 for the first try and 10 for the second, totalling in 15 seconds over 2 tries.
  */
case class LinearBackoff(duration: FiniteDuration) extends BackoffStrategy
