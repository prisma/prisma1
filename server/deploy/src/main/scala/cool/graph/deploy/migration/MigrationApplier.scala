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
  def applyMigration(previousProject: Project, nextProject: Project, migration: Migration): Future[Unit]
}

case class MigrationApplierImpl(clientDatabase: DatabaseDef)(implicit ec: ExecutionContext) extends MigrationApplier {

  override def applyMigration(previousProject: Project, nextProject: Project, migration: Migration): Future[Unit] = {
    val initialResult = Future.successful(())
    migration.steps.foldLeft(initialResult) { (previous, step) =>
      for {
        _ <- previous
        _ <- applyStep(previousProject, nextProject, step)
      } yield ()
    }
  }

  def applyStep(previousProject: Project, nextProject: Project, step: MigrationStep): Future[Unit] = {
    step match {
      case x: SetupProject =>
        executeClientMutaction(CreateClientDatabaseForProject(nextProject.id))

      case x: CreateModel =>
        executeClientMutaction(CreateModelTable(nextProject.id, x.name))

      case x: DeleteModel =>
        executeClientMutaction(DeleteModelTable(nextProject.id, x.name))

      case x: UpdateModel =>
        executeClientMutaction(RenameModelTable(projectId = nextProject.id, oldName = x.name, newName = x.newName))

      case x: CreateField =>
        val model = nextProject.getModelByName_!(x.name)
        val field = model.getFieldByName_!(x.name)
        executeClientMutaction(CreateColumn(nextProject.id, model, field))

      case x: DeleteField =>
        val model = nextProject.getModelByName_!(x.name)
        val field = model.getFieldByName_!(x.name)
        executeClientMutaction(DeleteColumn(nextProject.id, model, field))

      case x: UpdateField =>
        val model    = nextProject.getModelByName_!(x.model)
        val newField = nextProject.getFieldByName_!(x.model, x.finalName)
        val oldField = previousProject.getFieldByName_!(x.model, x.name)
        executeClientMutaction(UpdateColumn(nextProject.id, model, oldField, newField))

      case x: EnumMigrationStep =>
        println(s"migration step of type ${x.getClass.getSimpleName} does not need to be applied to the client database. Will do nothing.")
        Future.successful(())

      case x: CreateRelation =>
        val relation = nextProject.getRelationByName_!(x.name)
        executeClientMutaction(CreateRelationTable(nextProject, relation))

      case x: DeleteRelation =>
        val relation = nextProject.getRelationByName_!(x.name)
        executeClientMutaction(DeleteRelationTable(nextProject, relation))
//      case x =>
//        println(s"migration step of type ${x.getClass.getSimpleName} is not implemented yet. Will ignore it.")
//        Future.successful(())
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

    case Some(UnappliedMigration(prevProject, nextProject, migration)) =>
      println(s"found the unapplied migration in project ${prevProject.id}: $migration")
      val doit = for {
        _ <- applier.applyMigration(prevProject, nextProject, migration)
        _ <- projectPersistence.markMigrationAsApplied(migration)
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
