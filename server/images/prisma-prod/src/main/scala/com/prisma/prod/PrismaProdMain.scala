package com.prisma.prod

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.server.ApiServer
import com.prisma.deploy.server.ManagementServer
import com.prisma.subscriptions.SimpleSubscriptionsServer
import com.prisma.utils.boolean.BooleanUtils._
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaProdMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaProdDependencies()

  dependencies.initialize()(system.dispatcher)

  val port                    = dependencies.config.port.getOrElse(4466)
  val includeManagementServer = dependencies.config.managmentApiEnabled
  val servers = includeManagementServer.flatMap(_.toOption(ManagementServer("management", dependencies.config.server2serverSecret))) ++ List(
    WebsocketServer(dependencies),
    ApiServer(dependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer(),
    WorkerServer(dependencies)
  )

  ServerExecutor(
    port = port,
    servers = servers.toSeq: _*
  ).startBlocking()
}
