package cool.graph.deploy.migration

import akka.actor.Actor
import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.deploy.migration.MigrationApplierJob.ScanForUnappliedMigrations
import cool.graph.shared.models.UnappliedMigration
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future
import scala.util.{Failure, Success}

object MigrationApplierJob {
  object ScanForUnappliedMigrations
}

case class MigrationApplierJob(
    clientDatabase: DatabaseDef,
    migrationPersistence: MigrationPersistence
) extends Actor {
  import akka.pattern.pipe
  import context.dispatcher
  import scala.concurrent.duration._

  val applier = MigrationApplierImpl(clientDatabase)

  scheduleScanMessage

  override def receive: Receive = {
    case ScanForUnappliedMigrations =>
      println("scanning for migrations")
      pipe(migrationPersistence.getUnappliedMigration()) to self

    case Some(UnappliedMigration(prevProject, nextProject, migration)) =>
      println(s"found the unapplied migration in project ${prevProject.id}: $migration")
      val doit = for {
        result <- applier.applyMigration(prevProject, nextProject, migration)
        _ <- if (result.succeeded) {
              migrationPersistence.markMigrationAsApplied(migration)
            } else {
              Future.successful(())
            }
      } yield ()
      doit.onComplete {
        case Success(_) =>
          println("applying migration succeeded")
          scheduleScanMessage

        case Failure(e) =>
          println("applying migration failed with:")
          e.printStackTrace()
          scheduleScanMessage
      }

    case None =>
      println("found no unapplied migration")
      scheduleScanMessage

    case akka.actor.Status.Failure(throwable) =>
      println("piping failed with:")
      throwable.printStackTrace()
      scheduleScanMessage
  }

  def scheduleScanMessage = context.system.scheduler.scheduleOnce(10.seconds, self, ScanForUnappliedMigrations)
}
