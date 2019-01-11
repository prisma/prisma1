package com.prisma.deploy.migration.migrator

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence}
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.migration.migrator.DeploymentProtocol.{Initialize, Schedule}
import com.prisma.shared.models.{Function, Migration, MigrationStep, Schema}

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}

case class AsyncMigrator(
    migrationPersistence: MigrationPersistence,
    projectPersistence: ProjectPersistence,
    deployConnector: DeployConnector
)(
    implicit val system: ActorSystem,
    materializer: ActorMaterializer
) extends Migrator {
  import system.dispatcher

  lazy val deploymentScheduler = system.actorOf(Props(DeploymentSchedulerActor(migrationPersistence, projectPersistence, deployConnector)))
  implicit val timeout         = new Timeout(5.minutes)

  override def schedule(
      projectId: String,
      nextSchema: Schema,
      steps: Vector[MigrationStep],
      functions: Vector[Function],
      rawDataModel: String
  ): Future[Migration] = {
    (deploymentScheduler ? Schedule(projectId, nextSchema, steps, functions, rawDataModel)).mapTo[Migration]
  }

  override def initialize: Unit = {
    (deploymentScheduler ? Initialize).onComplete {
      case Success(_) =>
        println("Deployment worker initialization complete.")

      case Failure(err) =>
        println(s"Fatal error during deployment worker initialization: $err")
        sys.exit(-1)
    }
  }
}
