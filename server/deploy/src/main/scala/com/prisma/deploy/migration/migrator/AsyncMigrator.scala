package com.prisma.deploy.migration.migrator

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.prisma.deploy.database.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.migration.migrator.DeploymentProtocol.{Initialize, Schedule}
import com.prisma.deploy.persistence.DeployPersistencePlugin
import com.prisma.shared.models.{Function, Migration, MigrationStep, Schema}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class AsyncMigrator(
    clientDatabase: DatabaseDef,
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    persistencePlugin: DeployPersistencePlugin
)(
    implicit val system: ActorSystem,
    materializer: ActorMaterializer
) extends Migrator {
  import system.dispatcher

  val deploymentScheduler = system.actorOf(Props(DeploymentSchedulerActor(migrationPersistence, projectPersistence, clientDatabase, persistencePlugin)))
  implicit val timeout    = new Timeout(5.minutes)

  (deploymentScheduler ? Initialize).onComplete {
    case Success(_) =>
      println("Deployment worker initialization complete.")

    case Failure(err) =>
      println(s"Fatal error during deployment worker initialization: $err")
      sys.exit(-1)
  }

  override def schedule(projectId: String, nextSchema: Schema, steps: Vector[MigrationStep], functions: Vector[Function]): Future[Migration] = {
    (deploymentScheduler ? Schedule(projectId, nextSchema, steps, functions)).mapTo[Migration]
  }
}
