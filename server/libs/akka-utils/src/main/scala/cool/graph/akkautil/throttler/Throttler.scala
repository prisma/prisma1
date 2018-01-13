package cool.graph.akkautil.throttler

import java.util.concurrent.TimeUnit

import akka.actor.Status.Failure
import akka.actor.{Actor, ActorRef, ActorSystem, Props, ReceiveTimeout, Terminated}
import akka.contrib.throttle.Throttler.SetTarget
import akka.contrib.throttle.TimerBasedThrottler
import akka.pattern.AskTimeoutException
import cool.graph.akkautil.throttler.Throttler.{ThrottleBufferFullException, ThrottleCallTimeoutException, ThrottleResult}
import cool.graph.akkautil.throttler.ThrottlerManager.Requests.ThrottledCall

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.reflect.ClassTag

object Throttler {
  sealed abstract class ThrottlerException(msg: String) extends Exception(msg)
  class ThrottleBufferFullException(msg: String)        extends ThrottlerException(msg)
  class ThrottleCallTimeoutException(msg: String)       extends ThrottlerException(msg)

  case class ThrottleResult[T](result: T, throttledBy: Long)
}
// Todo - migrate: https://doc.akka.io/docs/akka/2.5.3/scala/project/migration-guide-2.4.x-2.5.x.html
case class Throttler[A](groupBy: A => Any, amount: Int, per: FiniteDuration, timeout: akka.util.Timeout, maxCallsInFlight: Int)(
    implicit actorSystem: ActorSystem) {

  import akka.pattern.ask
  implicit val implicitTimeout = timeout

  val throttlerActor = actorSystem.actorOf(ThrottlerManager.props(groupBy, amount, per, maxCallsInFlight))
  @throws[ThrottleCallTimeoutException]("thrown if the throttled call cannot be fulfilled within the given timeout")
  @throws[ThrottleBufferFullException]("thrown if the throttled call cannot be fulfilled in the given timeout")
  def throttled[B](groupBy: A)(call: () => Future[B])(implicit tag: ClassTag[B]): Future[ThrottleResult[B]] = {
    val askResult = throttlerActor ? ThrottledCall(call, groupBy)

    askResult
      .mapTo[ThrottleResult[B]]
      .recoverWith {
        case _: AskTimeoutException => Future.failed(new ThrottleCallTimeoutException(s"The call to the group [$groupBy] timed out."))
      }(actorSystem.dispatcher)
  }
}

object ThrottlerManager {
  object Requests {
    case class ThrottledCall[A, B](fn: () => Future[B], groupBy: A)
    case class ExecutableCall(call: () => Future[Any], sender: ActorRef, groupBy: Any)
    case class ExecuteCall(call: () => Future[Any], sender: ActorRef, created: Long)
  }

  def props[A](groupBy: A => Any, numberOfCalls: Int, duration: FiniteDuration, maxCallsInFlight: Int) = {
    Props(new ThrottlerManager(groupBy, akka.contrib.throttle.Throttler.Rate(numberOfCalls, duration), maxCallsInFlight))
  }
}

class ThrottlerManager[A](groupBy: A => Any, rate: akka.contrib.throttle.Throttler.Rate, maxCallsInFlight: Int) extends Actor {
  import cool.graph.akkautil.throttler.ThrottlerManager.Requests._

  val throttlerGroups: mutable.Map[Any, ActorRef] = mutable.Map.empty

  def receive = {
    case call @ ThrottledCall(_, _) =>
      val casted    = call.asInstanceOf[ThrottledCall[A, Any]]
      val throttler = getThrottler(casted.groupBy)
      throttler ! ExecutableCall(call.fn, sender, casted.groupBy)

    case Terminated(terminatedGroup) =>
      throttlerGroups.find {
        case (_, throttlerGroup) =>
          throttlerGroup == terminatedGroup
      } match {
        case Some((key, _)) =>
          throttlerGroups.remove(key)
        case None =>
          println(s"Tried to remove non-existing group $terminatedGroup")
      }
  }

  def getThrottler(arg: A): ActorRef = {
    val groupByResult = groupBy(arg)
    throttlerGroups.getOrElseUpdate(groupByResult, {
      val ref = context.actorOf(ThrottlerGroup.props(rate, maxCallsInFlight), groupByResult.toString)
      context.watch(ref)
      ref
    })
  }
}

object ThrottlerGroup {
  def props(rate: akka.contrib.throttle.Throttler.Rate, maxCallsInFlight: Int) = Props(new ThrottlerGroup(rate, maxCallsInFlight))
}

class ThrottlerGroup(rate: akka.contrib.throttle.Throttler.Rate, maxCallsInFlight: Int) extends Actor {
  import akka.pattern.pipe
  import context.dispatcher
  import cool.graph.akkautil.throttler.ThrottlerManager.Requests._

  var requestsInFlight = 0
  val akkaThrottler    = context.actorOf(Props(new TimerBasedThrottler(rate)))

  akkaThrottler ! SetTarget(Some(self))
  context.setReceiveTimeout(FiniteDuration(3, TimeUnit.MINUTES))

  override def receive: Receive = {
    case ExecutableCall(call, callSender, groupBy) =>
      if (requestsInFlight < maxCallsInFlight) {
        akkaThrottler ! ExecuteCall(call, callSender, created = System.currentTimeMillis)
        requestsInFlight += 1
      } else {
        callSender ! Failure(new ThrottleBufferFullException(s"Exceeded the limit of $maxCallsInFlight of in flight calls for groupBy [$groupBy]"))
      }

    case ExecuteCall(call, callSender, created) =>
      val throttledBy = System.currentTimeMillis() - created
      val result      = call().map(x => ThrottleResult(x, throttledBy))
      pipe(result) to callSender
      requestsInFlight -= 1

    case ReceiveTimeout =>
      context.stop(self)
  }
}
