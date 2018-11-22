package com.prisma.prod

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.prisma.image.{SangriaHandlerImpl, Version}
import com.prisma.sangria_server.AkkaHttpSangriaServer

object PrismaProdMain extends App {
  System.setProperty("org.jooq.no-logo", "true")

  implicit val system       = ActorSystem("single-server")
  implicit val materializer = ActorMaterializer()
  implicit val dependencies = PrismaProdDependencies()

  dependencies.initialize()
  Version.check()

  val sangriaHandler = SangriaHandlerImpl(managementApiEnabled = dependencies.config.managmentApiEnabled.getOrElse(false))
  val executor       = AkkaHttpSangriaServer
  val server         = executor.create(handler = sangriaHandler, port = dependencies.config.port.getOrElse(4466), requestPrefix = sys.env.getOrElse("ENV", "local"))

  server.startBlocking()
}
