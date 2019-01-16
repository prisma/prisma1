package com.prisma.deploy

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import com.prisma.auth.Auth
import com.prisma.config.PrismaConfig
import com.prisma.deploy.connector.DeployConnector
import com.prisma.deploy.connector.persistence.InternalMigration
import com.prisma.deploy.migration.migrator.Migrator
import com.prisma.deploy.schema.SchemaBuilder
import com.prisma.deploy.schema.mutations.FunctionValidator
import com.prisma.deploy.server.auth.ManagementAuth
import com.prisma.errors.ErrorReporter
import com.prisma.messagebus.PubSubPublisher
import com.prisma.shared.models.{Project, ProjectIdEncoder}
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.{ExecutionContext, Future}

trait DeployDependencies extends AwaitUtils {
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer
  implicit val reporter: ErrorReporter

  implicit def self: DeployDependencies

  def config: PrismaConfig
  def migrator: Migrator
  def managementAuth: ManagementAuth
  def invalidationPublisher: PubSubPublisher[String]
  def apiAuth: Auth
  def deployConnector: DeployConnector
  def functionValidator: FunctionValidator
  def projectIdEncoder: ProjectIdEncoder
  def telemetryActor: ActorRef

  lazy val projectPersistence      = deployConnector.projectPersistence
  lazy val migrationPersistence    = deployConnector.migrationPersistence
  lazy val managementSchemaBuilder = SchemaBuilder()

  def initialize()(implicit ec: ExecutionContext): Future[Unit] = {
    system.actorOf(Props(DatabaseSizeReporter(projectPersistence, deployConnector)))
    for {
      _ <- deployConnector.initialize()
      _ <- applyInternalMigrations
    } yield ()
  }

  private def applyInternalMigrations = {
    import system.dispatcher
    import com.prisma.utils.future.FutureUtils._

    def applyOneMigration(migration: InternalMigration, projects: Vector[Project]): Future[Unit] = {
      for {
        _ <- Future.sequence {
              projects.map(p => deployConnector.internalMigrationApplier.apply(migration, p))
            }
        _ <- deployConnector.internalMigrationPersistence.create(migration)
      } yield ()
    }

    for {
      appliedMigrations <- deployConnector.internalMigrationPersistence.loadAll()
      migrationsToApply = InternalMigration.values.toSet.diff(appliedMigrations.toSet)
      allProjects       <- if (migrationsToApply.nonEmpty) projectPersistence.loadAll() else Future.successful(Vector.empty)
      migrationThunks   = migrationsToApply.map(m => () => applyOneMigration(m, allProjects.toVector)).toVector
      _                 <- migrationThunks.runInChunksOf(10)
    } yield ()
  }
}
