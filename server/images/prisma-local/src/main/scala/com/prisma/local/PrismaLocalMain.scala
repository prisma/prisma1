package com.prisma.local

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.image.SangriaHandlerImpl
import com.prisma.sangria_server.AkkaHttpSangriaServer

object PrismaLocalMain extends App {
  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaLocalDependencies()

  dependencies.initialize()(system.dispatcher)

  Version.check()

  val sangriaHandler = SangriaHandlerImpl(managementApiEnabled = true)
  val executor       = AkkaHttpSangriaServer
//  val executor = BlazeSangriaServer
  val server = executor.create(handler = sangriaHandler, port = dependencies.config.port.getOrElse(4466), requestPrefix = sys.env.getOrElse("ENV", "local"))
  server.startBlocking()
}
