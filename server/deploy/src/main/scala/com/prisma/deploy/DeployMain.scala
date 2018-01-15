package com.prisma.deploy
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.deploy.server.ClusterServer

object DeployMain extends App {
  implicit val system       = ActorSystem("deploy-main")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = DeployDependenciesImpl()

  ServerExecutor(8081, ClusterServer("cluster")).startBlocking()
}
