package cool.graph.deploy.migration

import akka.actor.Actor
import akka.actor.Actor.Receive
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.MigrationApplierJob.ScanForUnappliedMigrations
import cool.graph.deploy.migration.mutactions._
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

      case x: DeleteModel =>
        executeClientMutaction(DeleteModelTable(project.id, x.name))

      case x: UpdateModel =>
        executeClientMutaction(RenameModelTable(projectId = project.id, oldName = x.name, newName = x.newName))

      case x: EnumMigrationStep =>
        println(s"migration step of type ${x.getClass.getSimpleName} does not need to be applied to the client database. Will do nothing.")
        Future.successful(())

      case x: CreateField =>
        val model = project.getModelByName_!(x.name)
        val field = model.getFieldByName_!(x.name)
        executeClientMutaction(CreateColumn(project.id, model, field))

      case x: DeleteField =>
        val model = project.getModelByName_!(x.name)
        val field = model.getFieldByName_!(x.name)
        executeClientMutaction(DeleteColumn(project.id, model, field))

      case x: UpdateField =>
        val oldProject = project // TODO: we need the old project here as well
        val model      = project.getModelByName_!(x.model)
        val newField   = project.getFieldByName_!(x.model, x.finalName)
        val oldField   = oldProject.getFieldByName_!(x.model, x.name)
        executeClientMutaction(UpdateColumn(project.id, model, oldField, newField))

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
