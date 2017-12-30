package cool.graph.deploy.migration.migrator

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import cool.graph.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import cool.graph.deploy.migration.MigrationApplier
import cool.graph.shared.models.{Migration, MigrationStep, Project}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class AsyncMigrator(
    clientDatabase: DatabaseDef,
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    applier: MigrationApplier
)(
    implicit val system: ActorSystem,
    materializer: ActorMaterializer
) extends Migrator {
  import system.dispatcher

  val deploymentScheduler = system.actorOf(Props(DeploymentSchedulerActor()(migrationPersistence, projectPersistence, applier)))
  implicit val timeout    = new Timeout(30.seconds)

  (deploymentScheduler ? Initialize).onComplete {
    case Success(_) =>
      println("Deployment worker initialization complete.")

    case Failure(err) =>
      println(s"Fatal error during deployment worker initialization: $err")
      sys.exit(-1)
  }

  override def schedule(nextProject: Project, steps: Vector[MigrationStep]): Future[Migration] = {
    (deploymentScheduler ? Schedule(nextProject, steps)).mapTo[Migration]
  }
}
