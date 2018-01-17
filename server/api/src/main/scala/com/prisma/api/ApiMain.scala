package com.prisma.api
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import com.prisma.akkautil.http.ServerExecutor
import com.prisma.api.schema.SchemaBuilder
import com.prisma.api.server.ApiServer
import com.prisma.messagebus.pubsub.inmemory.InMemoryAkkaPubSub

object ApiMain extends App with LazyLogging {
  implicit val system          = ActorSystem("api-main")
  implicit val materializer    = ActorMaterializer()
  implicit val apiDependencies = new ApiDependenciesImpl(InMemoryAkkaPubSub[String]())

  val schemaBuilder = SchemaBuilder()
  val server        = ApiServer(schemaBuilder = schemaBuilder)

  ServerExecutor(9000, server).startBlocking()
}
