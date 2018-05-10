package com.prisma.prod

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.{Server, ServerExecutor}
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

  val port              = dependencies.config.port.getOrElse(4466)
  val includeMgmtServer = dependencies.config.managmentApiEnabled

  def managementServers: List[Server] = {
    dependencies.migrator.initialize
    includeMgmtServer
      .flatMap(
        _.toOption(List(
          ManagementServer("management", dependencies.config.server2serverSecret),
          ManagementServer("cluster", dependencies.config.server2serverSecret) // Deprecated, will be removed soon
        )))
      .toList
      .flatten
  }

  val mgmtServer = includeMgmtServer
    .flatMap(_.toOption(managementServers))
    .toList
    .flatten

  val servers = mgmtServer ++ List(
    WebsocketServer(dependencies),
    ApiServer(dependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer(),
    WorkerServer(dependencies)
  )

  ServerExecutor(
    port = port,
    servers = servers: _*
  ).startBlocking()
}
