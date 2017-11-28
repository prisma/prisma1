package cool.graph.deploy.migration

import akka.actor.Actor
import akka.actor.Actor.Receive
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.MigrationApplierJob.ScanForUnappliedMigrations
import cool.graph.deploy.migration.mutactions.{ClientSqlMutaction, CreateClientDatabaseForProject, CreateModelTable}
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}

trait MigrationApplier {
  def applyMigration(project: Project, migration: MigrationSteps): Future[Unit]
}

case class MigrationApplierImpl(
    clientDatabase: DatabaseDef
)(implicit ec: ExecutionContext)
    extends MigrationApplier {
  override def applyMigration(project: Project, migration: MigrationSteps): Future[Unit] = {
    val initialResult = Future.successful(())

    if (project.revision == 1) {
      executeClientMutaction(CreateClientDatabaseForProject(project.id))
    } else {
      migration.steps.foldLeft(initialResult) { (previous, step) =>
        for {
          _ <- previous
          _ <- applyStep(project, step)
        } yield ()
      }
    }
  }

  def applyStep(project: Project, step: MigrationStep): Future[Unit] = {
    step match {
      case x: CreateModel =>
        executeClientMutaction(CreateModelTable(project.id, x.name))

      case x =>
        println(s"migration step of type ${x.getClass.getSimpleName} is not implemented yet. Will ignore it.")
        Future.successful(())
    }
  }

  def executeClientMutaction(mutaction: ClientSqlMutaction): Future[Unit] = {
    for {
      statements <- mutaction.execute
      _          <- clientDatabase.run(statements.sqlAction)
    } yield ()
  }
}

object MigrationApplierJob {
  object ScanForUnappliedMigrations
}

case class MigrationApplierJob(
    clientDatabase: DatabaseDef,
    projectPersistence: ProjectPersistence
) extends Actor {
  import scala.concurrent.duration._
  import akka.pattern.pipe
  import context.dispatcher

  val applier = MigrationApplierImpl(clientDatabase)

  scheduleScanMessage

  override def receive: Receive = {
    case ScanForUnappliedMigrations =>
      println("scanning for migrations")
      pipe(projectPersistence.getUnappliedMigration()) to self

    case Some(UnappliedMigration(project, migration)) =>
      println(s"found the unapplied migration in project ${project.id}: $migration")
      val doit = for {
        _ <- applier.applyMigration(project, migration)
        _ <- projectPersistence.markMigrationAsApplied(project, migration)
      } yield ()
      doit.onComplete { result =>
        println(s"applying migration resulted in:: $result")
        scheduleScanMessage
      }

    case None =>
      println("found no unapplied migration")
      scheduleScanMessage
  }

  def scheduleScanMessage = context.system.scheduler.scheduleOnce(10.seconds, self, ScanForUnappliedMigrations)
}
