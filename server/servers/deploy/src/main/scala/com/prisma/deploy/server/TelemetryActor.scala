package com.prisma.deploy.server

import akka.actor.Actor
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import com.prisma.deploy.connector.DeployConnector
import com.prisma.graphql.GraphQlClientImpl
import com.prisma.utils.await.AwaitUtils
import org.joda.time.DateTime

import scala.concurrent.duration._
import scala.util.{Failure, Success}

object TelemetryActor {
  object Report
}

case class TelemetryActor(connector: DeployConnector)(implicit val materializer: ActorMaterializer) extends Actor with AwaitUtils {
  import TelemetryActor._

  implicit val system = context.system
  implicit val ec     = context.system.dispatcher

  val info               = connector.getOrCreateTelemetryInfo().await()
  val version            = sys.env.getOrElse("CLUSTER_VERSION", "Unknown")
  val gqlClient          = GraphQlClientImpl("https://stats.prisma.io", Map.empty, Http())
  val regularInterval    = 1.hour
  val errorInterval      = 1.minute
  val initialGracePeriod = 10.seconds

  def initialDelay: FiniteDuration = info.lastPing match {
    case Some(lastPing) =>
      Math.max(regularInterval.toMillis - (new DateTime().getMillis - lastPing.getMillis), 0).millis + initialGracePeriod

    case None =>
      initialGracePeriod
  }

  context.system.scheduler.scheduleOnce(initialDelay, self, Report)

  override def receive: Receive = {
    case Report => report
  }

  private def report = {
    connector.projectPersistence
      .loadAll()
      .flatMap(projects => gqlClient.sendQuery(s"""mutation {ping(id: "${info.id}", services: ${projects.length}, version: "$version")}""".stripMargin))
      .onComplete {
        case Success(resp) =>
          if (resp.is2xx) {
            connector.updateTelemetryInfo(new DateTime()).onComplete {
              case Success(_) => context.system.scheduler.scheduleOnce(regularInterval, self, Report)
              case Failure(_) => context.system.scheduler.scheduleOnce(errorInterval, self, Report)
            }
          } else {
            println(s"[Telemetry] Warning: Telemetry call failed with status ${resp.status} | ${resp.body}")
            context.system.scheduler.scheduleOnce(errorInterval, self, Report)
          }

        case Failure(err) =>
          println(s"[Telemetry] Warning: Telemetry call failed with $err")
          context.system.scheduler.scheduleOnce(errorInterval, self, Report)
      }
  }
}
