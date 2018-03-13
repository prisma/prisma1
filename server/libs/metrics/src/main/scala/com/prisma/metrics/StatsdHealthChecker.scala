package com.prisma.metrics

import java.util.concurrent.atomic.AtomicBoolean

import akka.actor.{Actor, Props, Timers}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.akkautil.http.SimpleHttpClient

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}
import scala.concurrent.duration._

case class StatsdHealthChecker() {
  import StatsdHealthCheckerActor._
  import system.dispatcher

  implicit val system       = SingleThreadedActorSystem("statsd-health")
  implicit val materializer = ActorMaterializer()
  implicit val timeout      = new Timeout(5.minutes)

  val httpClient = SimpleHttpClient()
  val actor      = system.actorOf(Props(StatsdHealthCheckerActor(httpClient)))

  def isUp: Boolean =
    Await.result((actor ? Status).mapTo[Boolean].transformWith {
      case Success(bool) => Future.successful(bool)
      case Failure(e)    => Future.successful(false)
    }, 2.seconds)

  def newIP(ip: String) = actor ! NewIP(ip)
}

object StatsdHealthCheckerActor {
  case object TickKey
  case object Init
  case object Check
  case object Status
  case class NewIP(ip: String)
}

case class StatsdHealthCheckerActor(httpClient: SimpleHttpClient) extends Actor with Timers {
  import StatsdHealthCheckerActor._

  implicit val ec = context.system.dispatcher

  private var ipToCheck  = ""
  private val statsdIsUp = new AtomicBoolean(true)
  private val port       = sys.env.getOrElse("METRICS_HEALTH_PORT", sys.error("METRICS_HEALTH_PORT env var required but not found."))

  def receive = {
    case Check =>
      checkAndWriteHealthStatus()

    case NewIP(ip) =>
      ipToCheck = ip
      timers.cancel(TickKey)
      checkAndWriteHealthStatus()
      timers.startPeriodicTimer(TickKey, Check, 5.seconds) // Check health every 5 seconds

    case Status =>
      sender ! statsdIsUp
  }

  private def checkAndWriteHealthStatus() = {
    httpClient.get(s"http://$ipToCheck:$port", timeout = 2.seconds).onComplete {
      case Success(resp) => statsdIsUp.set(true)
      case Failure(e)    => statsdIsUp.set(false)
    }
  }
}
