package com.prisma.local

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.ApiMetrics
import com.prisma.api.server.ApiServer
import com.prisma.deploy.DeployMetrics
import com.prisma.deploy.server.ManagementServer
import com.prisma.metrics.CustomTag
import com.prisma.subscriptions.SimpleSubscriptionsServer
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaLocalMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaLocalDependencies()

  dependencies.initialize()(system.dispatcher)
  dependencies.migrator.initialize

  DummyValues.create(system)

  Version.check()

  ServerExecutor(
    port = dependencies.config.port.getOrElse(4466),
    ManagementServer("management", dependencies.config.server2serverSecret),
    ManagementServer("cluster", dependencies.config.server2serverSecret), // Deprecated, will be removed soon
    WebsocketServer(dependencies),
    ApiServer(dependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer(),
    WorkerServer(dependencies)
  ).startBlocking()
}

object DummyValues {
  val movingProject              = "moving-project"
  val constantProject            = "constant-project"
  val movingProjectSizeGauge     = DeployMetrics.defineGauge("projectDatabase.sizeInMb", (CustomTag("projectId"), movingProject))
  val constantProjectSizeGauge   = DeployMetrics.defineGauge("projectDatabase.sizeInMb", (CustomTag("projectId"), constantProject))
  val movingRequestTimeMax       = 1000 // 1s
  val movingRequestCountMax      = 100 // per 5s => 20/s
  val movingSubscriptionEventMax = 10 // per 5s => 20/s
  val movingDatabaseSizeMax      = 500

  def create(system: ActorSystem) = {
    import scala.concurrent.duration._
    import system.dispatcher
    val i = new AtomicInteger(0)

    def fractionForTick(tick: Int) = {
      val windowForMovingValues = 60
      val turnAroundPoint       = windowForMovingValues / 2
      val n                     = (tick % windowForMovingValues).toFloat
      if (n < turnAroundPoint) {
        n / turnAroundPoint // the first 30 ticks go slowly up from 0 to 1
      } else {
        (windowForMovingValues - n) / turnAroundPoint // the next 30 ticks go slowly from 1 to 0
      }
    }

    system.scheduler.schedule(5.seconds, 5.seconds) {
      val fraction = fractionForTick(i.incrementAndGet())
      println(s"""i: ${i.get} fraction: ${fraction}""")
      // request time
      ApiMetrics.requestDuration.record(math.round(fraction * movingRequestTimeMax), Vector(movingProject))
      ApiMetrics.requestDuration.record(500, Vector(constantProject))
      // request count
      ApiMetrics.requestCounter.incBy(math.round(fraction * movingRequestCountMax), movingProject)
      ApiMetrics.requestCounter.incBy(5, constantProject)
      // subscriptions
      ApiMetrics.subscriptionEventCounter.incBy(math.round(fraction * movingSubscriptionEventMax), movingProject)
      ApiMetrics.subscriptionEventCounter.inc(constantProject)
      // project db size
      movingProjectSizeGauge.set(math.round(fraction * movingDatabaseSizeMax))
      constantProjectSizeGauge.set(100)

    }
  }
}
