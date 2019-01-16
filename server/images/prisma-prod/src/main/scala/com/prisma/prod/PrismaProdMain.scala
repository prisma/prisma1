package com.prisma.prod

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.image.SangriaHandlerImpl
import com.prisma.sangria_server.AkkaHttpSangriaServer

object PrismaProdMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaProdDependencies()

  val initResult = dependencies.initialize()(system.dispatcher)
  Await.result(initResult, Duration.Inf)

  val sangriaHandler = SangriaHandlerImpl(managementApiEnabled = dependencies.config.managmentApiEnabled.getOrElse(false))
  val executor       = AkkaHttpSangriaServer
  //  val executor = BlazeSangriaServer
  val server = executor.create(handler = sangriaHandler, port = dependencies.config.port.getOrElse(4466), requestPrefix = sys.env.getOrElse("ENV", "local"))
  server.startBlocking()
}
