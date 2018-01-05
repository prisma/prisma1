package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.deploy.migration.MigrationStepMapper
import cool.graph.deploy.migration.migrator.Migrator
import cool.graph.deploy.migration.mutactions.ClientSqlMutaction
import cool.graph.shared.models._
import cool.graph.utils.await.AwaitUtils
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.Future

case class TestMigrator(
    clientDatabase: DatabaseDef,
    internalDb: DatabaseDef,
    migrationPersistence: MigrationPersistence
)(implicit val system: ActorSystem)
    extends Migrator
    with AwaitUtils {
  import system.dispatcher

  // Todo this is temporary, a real implementation is required
  // For tests, the schedule directly does all the migration work to remove asy
  override def schedule(projectId: String, nextSchema: Schema, steps: Vector[MigrationStep]): Future[Migration] = {
    val stepMapper = MigrationStepMapper(projectId)
    val result: Future[Migration] = for {
      savedMigration <- migrationPersistence.create(Migration(projectId, nextSchema, steps))
      lastMigration  <- migrationPersistence.getLastMigration(projectId)
      applied <- applyMigration(lastMigration.get.schema, savedMigration, stepMapper).flatMap { result =>
                  if (result.succeeded) {
                    migrationPersistence.updateMigrationStatus(savedMigration, MigrationStatus.Success).map { _ =>
                      savedMigration.copy(status = MigrationStatus.Success)
                    }
                  } else {
                    Future.failed(new Exception("applyMigration resulted in an error"))
                  }
                }
    } yield {
      applied
    }

    result.await
    result
  }

  def applyMigration(previousSchema: Schema, migration: Migration, mapper: MigrationStepMapper): Future[MigrationApplierResult] = {
    val initialProgress = MigrationProgress(pendingSteps = migration.steps, appliedSteps = Vector.empty, isRollingback = false)
    recurse(previousSchema, migration.schema, initialProgress, mapper)
  }

  def recurse(previousSchema: Schema, nextSchema: Schema, progress: MigrationProgress, mapper: MigrationStepMapper): Future[MigrationApplierResult] = {
    if (!progress.isRollingback) {
      recurseForward(previousSchema, nextSchema, progress, mapper)
    } else {
      recurseForRollback(previousSchema, nextSchema, progress, mapper)
    }
  }

  def recurseForward(previousSchema: Schema, nextSchema: Schema, progress: MigrationProgress, mapper: MigrationStepMapper): Future[MigrationApplierResult] = {
    if (progress.pendingSteps.nonEmpty) {
      val (step, newProgress) = progress.popPending

      val result = for {
        _ <- applyStep(previousSchema, nextSchema, step, mapper)
        x <- recurse(previousSchema, nextSchema, newProgress, mapper)
      } yield x

      result.recoverWith {
        case exception =>
          println("encountered exception while applying migration. will roll back.")
          exception.printStackTrace()
          recurseForRollback(previousSchema, nextSchema, newProgress.markForRollback, mapper)
      }
    } else {
      Future.successful(MigrationApplierResult(succeeded = true))
    }
  }

  def recurseForRollback(previousSchema: Schema,
                         nextSchema: Schema,
                         progress: MigrationProgress,
                         mapper: MigrationStepMapper): Future[MigrationApplierResult] = {
    if (progress.appliedSteps.nonEmpty) {
      val (step, newProgress) = progress.popApplied

      for {
        _ <- unapplyStep(previousSchema, nextSchema, step, mapper).recover { case _ => () }
        x <- recurse(previousSchema, nextSchema, newProgress, mapper)
      } yield x
    } else {
      Future.successful(MigrationApplierResult(succeeded = false))
    }
  }

  def applyStep(previousSchema: Schema, nextSchema: Schema, step: MigrationStep, mapper: MigrationStepMapper): Future[Unit] = {
    mapper.mutactionFor(previousSchema, nextSchema, step).map(executeClientMutaction).getOrElse(Future.successful(()))
  }

  def unapplyStep(previousSchema: Schema, nextSchema: Schema, step: MigrationStep, mapper: MigrationStepMapper): Future[Unit] = {
    mapper.mutactionFor(previousSchema, nextSchema, step).map(executeClientMutactionRollback).getOrElse(Future.successful(()))
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

case class MigrationApplierResult(succeeded: Boolean)
