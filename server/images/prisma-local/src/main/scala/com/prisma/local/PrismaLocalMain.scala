package com.prisma.local

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.server.ApiServer
import com.prisma.deploy.server.ManagementServer
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaLocalMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaLocalDependencies()

  dependencies.initialize()(system.dispatcher)
  dependencies.migrator.initialize

  Version.check()

  ServerExecutor(
    port = dependencies.config.port.getOrElse(4466),
    ManagementServer("management"),
    WebsocketServer(dependencies),
    ApiServer(dependencies.apiSchemaBuilder),
    WorkerServer(dependencies)
  ).startBlocking()
}
