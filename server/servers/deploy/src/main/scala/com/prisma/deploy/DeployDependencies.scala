package com.prisma.deploy

import java.util.concurrent.atomic.AtomicInteger

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
import org.joda.time.DateTime

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

    val finished = new AtomicInteger()

    def applyOneMigration(migration: InternalMigration, projects: Vector[Project]): Future[Unit] = {
      for {
        _ <- Future.sequence {
              projects.map { project =>
                deployConnector.internalMigrationApplier.apply(migration, project).map { _ =>
                  val result = finished.incrementAndGet()
                  if (result % 100 == 0 || result < 10) {
                    println(s"Finished migration of $finished projects")
                  }
                }
              }
            }
        _ <- deployConnector.internalMigrationPersistence.create(migration)
      } yield ()
    }

    println("starting internal migrations: " + new DateTime())
    for {
      appliedMigrations <- deployConnector.internalMigrationPersistence.loadAll()
      migrationsToApply = InternalMigration.values.toSet.diff(appliedMigrations.toSet)
      offset            = 12020
      allProjects       <- if (migrationsToApply.nonEmpty) projectPersistence.loadAll().map(_.slice(offset, offset + 1000)) else Future.successful(Vector.empty)
      _                 = println("fetched projects: " + new DateTime())
//      _                 = println(s"Will migrate the following project ids: ${allProjects.map(_.id)}")
      migrationThunks = migrationsToApply.map(m => () => applyOneMigration(m, allProjects.toVector)).toVector
      _               <- migrationThunks.runInChunksOf(2)
    } yield {
      println("finished internal migrations: " + new DateTime())
      ()
    }
  }
}
