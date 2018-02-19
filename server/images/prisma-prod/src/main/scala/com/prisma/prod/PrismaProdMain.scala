package com.prisma.prod

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.server.ApiServer
import com.prisma.deploy.server.ClusterServer
import com.prisma.subscriptions.SimpleSubscriptionsServer
import com.prisma.websocket.WebsocketServer
import com.prisma.workers.WorkerServer

object PrismaProdMain {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  val port                  = sys.env.getOrElse("PORT", "9000").toInt
  implicit val dependencies = PrismaProdDependencies()

  // TODO: add if for inclusion of cluster server

  ServerExecutor(
    port = port,
    ClusterServer("cluster"),
    WebsocketServer(dependencies),
    ApiServer(dependencies.apiSchemaBuilder),
    SimpleSubscriptionsServer(),
    WorkerServer(dependencies)
  ).startBlocking()
}
