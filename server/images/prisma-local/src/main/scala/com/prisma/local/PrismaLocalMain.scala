package com.prisma.local

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.server.ApiServer
import com.prisma.deploy.server.ManagementServer
import com.prisma.image.SangriaHandlerImpl
import com.prisma.sangria_server.AkkaHttpSangriaServer
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaLocalMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaLocalDependencies()

  dependencies.initialize()(system.dispatcher)
  dependencies.migrator.initialize

  Version.check()

  // FIXME: still need to start the worker server
  val sangriaHandler = SangriaHandlerImpl()
  val executor       = AkkaHttpSangriaServer
  val server         = executor.create(handler = sangriaHandler, port = dependencies.config.port.getOrElse(4466), requestPrefix = sys.env.getOrElse("ENV", "local"))
  server.startBlocking()

//  ServerExecutor(
//    port = dependencies.config.port.getOrElse(4466),
//    ManagementServer("management"),
//    ManagementServer("cluster"), // Deprecated, will be removed soon
//    WebsocketServer(dependencies),
//    ApiServer(dependencies.apiSchemaBuilder),
//    WorkerServer(dependencies)
//  ).startBlocking()
}
