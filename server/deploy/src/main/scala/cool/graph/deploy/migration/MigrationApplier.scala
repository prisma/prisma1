package cool.graph.deploy.migration

import akka.actor.Actor
import akka.actor.Actor.Receive
import cool.graph.deploy.database.persistence.ProjectPersistence
import cool.graph.deploy.migration.MigrationApplierJob.ScanForUnappliedMigrations
import cool.graph.deploy.migration.mutactions._
import cool.graph.shared.models._
import slick.dbio.DBIOAction
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait MigrationApplier {
  def applyMigration(project: Project, migration: MigrationSteps): Future[MigrationApplierResult]
}
case class MigrationApplierResult(succeeded: Boolean)

case class MigrationApplierImpl(
    clientDatabase: DatabaseDef
)(implicit ec: ExecutionContext)
    extends MigrationApplier {

  override def applyMigration(project: Project, migration: MigrationSteps): Future[MigrationApplierResult] = {
    if (project.revision == 1) {
      executeClientMutaction(CreateClientDatabaseForProject(project.id)).map(_ => MigrationApplierResult(succeeded = true))
    } else {
      val initialProgress = MigrationProgress(pendingSteps = migration.steps, appliedSteps = Vector.empty, isRollingback = false)
      recurse(project, initialProgress)
    }
  }

  def recurse(project: Project, progress: MigrationProgress): Future[MigrationApplierResult] = {
    if (!progress.isRollingback) {
      recurseForward(project, progress)
    } else {
      recurseForRollback(project, progress)
    }
  }

  def recurseForward(project: Project, progress: MigrationProgress): Future[MigrationApplierResult] = {
    if (progress.pendingSteps.nonEmpty) {
      val (step, newProgress) = progress.popPending

      val result = for {
        _ <- applyStep(project, step)
        x <- recurse(project, newProgress)
      } yield x

      result.recoverWith {
        case exception =>
          println("encountered exception while applying migration. will roll back.")
          exception.printStackTrace()
          recurseForRollback(project, newProgress.markForRollback)
      }
    } else {
      Future.successful(MigrationApplierResult(succeeded = true))
    }
  }

  def recurseForRollback(project: Project, progress: MigrationProgress): Future[MigrationApplierResult] = {
    if (progress.appliedSteps.nonEmpty) {
      val (step, newProgress) = progress.popApplied

      for {
        _ <- unapplyStep(project, step).recover { case _ => () }
        x <- recurse(project, newProgress)
      } yield x
    } else {
      Future.successful(MigrationApplierResult(succeeded = false))
    }
  }

  def applyStep(project: Project, step: MigrationStep): Future[Unit] = {
    migrationStepToMutaction(project, step).map(executeClientMutaction).getOrElse(Future.successful(()))
  }

  def unapplyStep(project: Project, step: MigrationStep): Future[Unit] = {
    migrationStepToMutaction(project, step).map(executeClientMutactionRollback).getOrElse(Future.successful(()))
  }

  def migrationStepToMutaction(project: Project, step: MigrationStep): Option[ClientSqlMutaction] = step match {
    case x: CreateModel =>
      Some(CreateModelTable(project.id, x.name))

    case x: DeleteModel =>
      Some(DeleteModelTable(project.id, x.name))

    case x: UpdateModel =>
      Some(RenameModelTable(projectId = project.id, oldName = x.name, newName = x.newName))

    case x: CreateField =>
      val model = project.getModelByName_!(x.model)
      val field = model.getFieldByName_!(x.name)
      if (field.isSystemField || !field.isScalar) {
        None
      } else {
        Some(CreateColumn(project.id, model, field))
      }

    case x: DeleteField =>
      val model = project.getModelByName_!(x.name)
      val field = model.getFieldByName_!(x.name)
      Some(DeleteColumn(project.id, model, field))

    case x: UpdateField =>
      val oldProject = project // TODO: we need the old project here as well
      val model      = project.getModelByName_!(x.model)
      val newField   = project.getFieldByName_!(x.model, x.finalName)
      val oldField   = oldProject.getFieldByName_!(x.model, x.name)
      Some(UpdateColumn(project.id, model, oldField, newField))

    case x: EnumMigrationStep =>
      println(s"migration step of type ${x.getClass.getSimpleName} does not need to be applied to the client database. Will do nothing.")
      None

    case x: CreateRelation =>
      val relation = project.getRelationByName_!(x.name)
      Some(CreateRelationTable(project, relation))

    case x: DeleteRelation =>
      val relation = project.getRelationByName_!(x.name)
      Some(DeleteRelationTable(project, relation))
  }

  def executeClientMutaction(mutaction: ClientSqlMutaction): Future[Unit] = {
    for {
      statements <- mutaction.execute
      _          <- clientDatabase.run(statements.sqlAction)
    } yield ()
  }

  def executeClientMutactionRollback(mutaction: ClientSqlMutaction): Future[Unit] = {
    for {
      statements <- mutaction.rollback.get
      _          <- clientDatabase.run(statements.sqlAction)
    } yield ()
  }

//  private val emptyMutaction = new ClientSqlMutaction {
//    val emptyResult = Future(ClientSqlStatementResult[Any](DBIOAction.successful(())))
//
//    override def execute: Future[ClientSqlStatementResult[Any]]          = emptyResult
//    override def rollback: Option[Future[ClientSqlStatementResult[Any]]] = Some(emptyResult)
//  }
}

case class MigrationProgress(
    appliedSteps: Vector[MigrationStep],
    pendingSteps: Vector[MigrationStep],
    isRollingback: Boolean
) {
  def addAppliedStep(step: MigrationStep) = copy(appliedSteps = appliedSteps :+ step)

  def popPending: (MigrationStep, MigrationProgress) = {
    val step = pendingSteps.head
    step -> copy(appliedSteps = appliedSteps :+ step, pendingSteps = pendingSteps.tail)
  }

  def popApplied: (MigrationStep, MigrationProgress) = {
    val step = appliedSteps.last
    step -> copy(appliedSteps = appliedSteps.dropRight(1))
  }

  def markForRollback = copy(isRollingback = true)
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
        result <- applier.applyMigration(project, migration)
        _ <- if (result.succeeded) {
              projectPersistence.markMigrationAsApplied(project, migration)
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
