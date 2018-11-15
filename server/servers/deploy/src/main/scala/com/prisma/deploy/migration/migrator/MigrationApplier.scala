package com.prisma.deploy.migration.migrator

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.persistence.MigrationPersistence
import com.prisma.shared.models.{Migration, MigrationStatus, MigrationStep, Schema}
import com.prisma.utils.exceptions.StackTraceUtils
import org.joda.time.DateTime

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success}

trait MigrationApplier {
  def apply(previousSchema: Schema, migration: Migration): Future[MigrationApplierResult]
}
case class MigrationApplierResult(succeeded: Boolean)

case class MigrationApplierImpl(
    migrationPersistence: MigrationPersistence,
    migrationStepMapper: MigrationStepMapper,
    mutactionExecutor: DeployMutactionExecutor
)(implicit ec: ExecutionContext)
    extends MigrationApplier {

  override def apply(previousSchema: Schema, migration: Migration): Future[MigrationApplierResult] = {
    for {
      _         <- Future.unit
      nextState = if (migration.status == MigrationStatus.Pending) MigrationStatus.InProgress else migration.status
      _         <- migrationPersistence.updateMigrationStatus(migration.id, nextState)
      _         <- migrationPersistence.updateStartedAt(migration.id, DateTime.now())
      result    <- startRecurse(previousSchema, migration)
      _         <- migrationPersistence.updateFinishedAt(migration.id, DateTime.now())
    } yield result
  }

  def startRecurse(previousSchema: Schema, migration: Migration): Future[MigrationApplierResult] = {
    if (!migration.isRollingBack) {
      recurseForward(previousSchema, migration)
    } else {
      recurseForRollback(previousSchema, migration)
    }
  }

  def recurseForward(previousSchema: Schema, migration: Migration): Future[MigrationApplierResult] = {
    if (migration.pendingSteps.nonEmpty) {
      val result = for {
        _             <- applyStep(previousSchema, migration, migration.currentStep)
        nextMigration = migration.incApplied
        _             <- migrationPersistence.updateMigrationApplied(migration.id, nextMigration.applied)
        x             <- recurseForward(previousSchema, nextMigration)
      } yield x

      result.recoverWith {
        case exception =>
          println(s"Encountered exception while applying migration. Rolling back. $exception")
          exception.printStackTrace()

          for {
            _             <- migrationPersistence.updateMigrationStatus(migration.id, MigrationStatus.RollingBack)
            _             <- migrationPersistence.updateMigrationErrors(migration.id, migration.errors :+ StackTraceUtils.print(exception))
            applierResult <- recurseForRollback(previousSchema, migration.copy(status = MigrationStatus.RollingBack))
          } yield applierResult
      }
    } else {
      migrationPersistence.updateMigrationStatus(migration.id, MigrationStatus.Success).map(_ => MigrationApplierResult(succeeded = true))
    }
  }

  def recurseForRollback(previousSchema: Schema, migration: Migration): Future[MigrationApplierResult] = {
    def continueRollback = {
      val nextMigration = migration.incRolledBack
      for {
        _ <- migrationPersistence.updateMigrationRolledBack(migration.id, nextMigration.rolledBack)
        x <- recurseForRollback(previousSchema, nextMigration)
      } yield x
    }
    def abortRollback(err: Throwable) = {
      println(s"Encountered exception while rolling back migration. Aborting. $err")
      val failedMigration = migration.markAsRollBackFailure
      for {
        _ <- migrationPersistence.updateMigrationStatus(migration.id, failedMigration.status)
        _ <- migrationPersistence.updateMigrationErrors(migration.id, failedMigration.errors :+ StackTraceUtils.print(err))
      } yield MigrationApplierResult(succeeded = false)
    }

    if (migration.pendingRollBackSteps.nonEmpty) {
      unapplyStep(previousSchema, migration, migration.pendingRollBackSteps.head).transformWith {
        case Success(_)   => continueRollback
        case Failure(err) => abortRollback(err)
      }
    } else {
      migrationPersistence.updateMigrationStatus(migration.id, MigrationStatus.RollbackSuccess).map(_ => MigrationApplierResult(succeeded = false))
    }
  }

  def applyStep(previousSchema: Schema, migration: Migration, step: MigrationStep): Future[Unit] = {
    migrationStepMapper.mutactionFor(previousSchema, migration.schema, step) match {
      case x if x.isEmpty =>
        Future.unit

      case list =>
        list.foldLeft(Future.unit) { (prev, mutaction) =>
          for {
            _ <- prev
            _ <- executeClientMutaction(mutaction)
          } yield ()
        }
    }
  }

  def unapplyStep(previousSchema: Schema, migration: Migration, step: MigrationStep): Future[Unit] = {
    migrationStepMapper.mutactionFor(previousSchema, migration.schema, step) match {
      case x if x.isEmpty => Future.unit
      case list           => Future.sequence(list.map(executeClientMutactionRollback)).map(_ => ())
    }
  }

  def executeClientMutaction(mutaction: DeployMutaction): Future[Unit] = mutactionExecutor.execute(mutaction)

  def executeClientMutactionRollback(mutaction: DeployMutaction): Future[Unit] = mutactionExecutor.rollback(mutaction)
}
