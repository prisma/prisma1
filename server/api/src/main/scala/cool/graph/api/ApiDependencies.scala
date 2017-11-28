package cool.graph.api

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.config.{Config, ConfigFactory}
import cool.graph.api.database.DatabaseConnectionManager

trait ApiDependencies {
  val config: Config = ConfigFactory.load()
  def destroy        = println("ApiDependencies [DESTROY]")

  val system: ActorSystem
  val materializer: ActorMaterializer

  val databaseManager: DatabaseConnectionManager
}

case class ApiDependenciesImpl(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override val databaseManager = DatabaseConnectionManager.initializeForSingleRegion(config)
}

case class ApiDependenciesForTest(implicit val system: ActorSystem, val materializer: ActorMaterializer) extends ApiDependencies {
  override val databaseManager = DatabaseConnectionManager.initializeForSingleRegion(config)
}
