package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.api.database.DatabaseConnectionManager

trait ApiDependencies {
  val config: Config = ConfigFactory.load()
  def destroy        = println("ApiDependencies [DESTROY]")

  val databaseManager: DatabaseConnectionManager
}

class ApiDependenciesImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override val databaseManager = DatabaseConnectionManager.initializeForSingleRegion(config)
}

class ApiDependenciesForTest(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override val databaseManager = DatabaseConnectionManager.initializeForSingleRegion(config)
}
