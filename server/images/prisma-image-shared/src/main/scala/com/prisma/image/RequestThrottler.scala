package com.prisma.image

import akka.actor.ActorSystem
import com.prisma.akkautil.throttler.Throttler
import com.prisma.akkautil.throttler.Throttler.ThrottleBufferFullException
import com.prisma.api.schema.CommonErrors.ThrottlerBufferFull
import com.prisma.sangria_server.Response
import com.prisma.shared.models.Project
import com.prisma.util.env.EnvUtils

import scala.concurrent.Future

case class RequestThrottler()(implicit system: ActorSystem) {
  import com.prisma.utils.future.FutureUtils._
  import system.dispatcher

  import scala.concurrent.duration._

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

  def throttleCallIfNeeded(projectId: String, isManagementApiRequest: Boolean)(fn: => Future[Response]): Future[Response] = {
    val mustBeThrottled = !isManagementApiRequest && !unthrottledProjectIds.contains(projectId)
    throttler match {
      case Some(throttler) if mustBeThrottled =>
        throttledCall(projectId, throttler, fn)

      case _ =>
        fn
    }
  }

  private def throttledCall(projectId: String, throttler: Throttler[String], fn: => Future[Response]): Future[Response] = {
    val result = throttler.throttled(projectId) { () =>
      fn
    }
    result.toFutureTry.map {
      case scala.util.Success(result) =>
        result.result.copy(
          headers = result.result.headers ++ Map("Throttled-By" -> (result.throttledBy.toString + "ms"))
        )
      case scala.util.Failure(_: ThrottleBufferFullException) =>
        throw ThrottlerBufferFull()

      case scala.util.Failure(exception) =>
        throw exception
    }
  }
}
