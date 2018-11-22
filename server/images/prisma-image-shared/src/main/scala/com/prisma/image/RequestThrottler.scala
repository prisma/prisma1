package com.prisma.image

import akka.actor.ActorSystem
import com.prisma.akkautil.throttler.Throttler
import com.prisma.akkautil.throttler.Throttler.ThrottleBufferFullException
import com.prisma.api.schema.CommonErrors.ThrottlerBufferFull
import com.prisma.shared.models.Project
import com.prisma.util.env.EnvUtils
import play.api.libs.json.JsValue

import scala.concurrent.Future

case class RequestThrottler()(implicit system: ActorSystem) {
  import com.prisma.utils.future.FutureUtils._
  import scala.concurrent.duration._
  import system.dispatcher

  lazy val unthrottledProjectIds = sys.env.get("UNTHROTTLED_PROJECT_IDS") match {
    case Some(envValue) => envValue.split('|').filter(_.nonEmpty).toVector
    case None           => Vector.empty
  }

  lazy val throttler: Option[Throttler[String]] = {
    for {
      throttlingRate    <- EnvUtils.asInt("THROTTLING_RATE")
      maxCallsInFlights <- EnvUtils.asInt("THROTTLING_MAX_CALLS_IN_FLIGHT")
    } yield {
      val per = EnvUtils.asInt("THROTTLING_RATE_PER_SECONDS").getOrElse(1)
      Throttler[String](
        groupBy = identity,
        amount = throttlingRate,
        per = per.seconds,
        timeout = 25.seconds,
        maxCallsInFlight = maxCallsInFlights.toInt
      )
    }
  }

  def throttleCallIfNeeded(project: Project)(fn: => Future[JsValue]) = {
    throttler match {
      case Some(throttler) if !unthrottledProjectIds.contains(project.id) => throttledCall(project, throttler, fn)
      case _                                                              => fn
    }
  }

  private def throttledCall(project: Project, throttler: Throttler[String], fn: => Future[JsValue]) = {
    val result = throttler.throttled(project.id) { () =>
      fn
    }
    result.toFutureTry.map {
      case scala.util.Success(result) =>
        // TODO: do we really need this?
        //        respondWithHeader(RawHeader("Throttled-By", result.throttledBy.toString + "ms")) {
        //          complete(result.result)
        //        }
        result.result

      case scala.util.Failure(_: ThrottleBufferFullException) =>
        throw ThrottlerBufferFull()

      case scala.util.Failure(exception) =>
        throw exception
    }
  }
}
