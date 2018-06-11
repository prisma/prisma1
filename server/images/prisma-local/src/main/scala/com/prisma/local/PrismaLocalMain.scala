package com.prisma.local

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.ApiMetrics
import com.prisma.api.server.ApiServer
import com.prisma.deploy.server.ManagementServer
import com.prisma.subscriptions.SimpleSubscriptionsServer
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaLocalMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaLocalDependencies()

  dependencies.initialize()(system.dispatcher)
  dependencies.migrator.initialize

  val projectId = "my-project-id"
  import scala.concurrent.duration._
  import system.dispatcher
  val i = new AtomicInteger(0)
  system.scheduler.schedule(5.seconds, 5.seconds) {
    println(i.get())
    ApiMetrics.requestCounter.inc(projectId)
    ApiMetrics.requestDuration.record(i.incrementAndGet(), Vector(projectId))
  }

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
