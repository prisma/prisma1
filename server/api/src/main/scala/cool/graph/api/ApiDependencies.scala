package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.api.database.DatabaseConnectionManager
import cool.graph.api.schema.SchemaBuilder

trait ApiDependencies {
  val config: Config = ConfigFactory.load()

  val apiSchemaBuilder: SchemaBuilder
  val databaseManager: DatabaseConnectionManager

  def destroy = println("ApiDependencies [DESTROY]")
}

class ApiDependenciesImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  val databaseManager  = DatabaseConnectionManager.initializeForSingleRegion(config)
  val apiSchemaBuilder = SchemaBuilder()
}

class ApiDependenciesForTest(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  val databaseManager  = DatabaseConnectionManager.initializeForSingleRegion(config)
  val apiSchemaBuilder = SchemaBuilder()
}
