package com.prisma.deploy.migration.migrator

import com.prisma.deploy.connector._
import com.prisma.deploy.connector.persistence.{MigrationPersistence, ProjectPersistence}
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
    projectPersistence: ProjectPersistence,
    migrationStepMapper: MigrationStepMapper,
    mutactionExecutor: DeployMutactionExecutor,
    databaseInspector: DatabaseInspector
)(implicit ec: ExecutionContext)
    extends MigrationApplier {

  override def apply(previousSchema: Schema, migration: Migration): Future[MigrationApplierResult] = {
    for {
      project   <- projectPersistence.load(migration.projectId)
      tables    <- databaseInspector.inspect(project.get.dbName)
      nextState = if (migration.status == MigrationStatus.Pending) MigrationStatus.InProgress else migration.status
      _         <- migrationPersistence.updateMigrationStatus(migration.id, nextState)
      _         <- migrationPersistence.updateStartedAt(migration.id, DateTime.now())
      result    <- startRecurse(previousSchema, migration, tables)
      _         <- migrationPersistence.updateFinishedAt(migration.id, DateTime.now())
    } yield result
  }

  def startRecurse(previousSchema: Schema, migration: Migration, databaseSchema: DatabaseSchema): Future[MigrationApplierResult] = {
    if (!migration.isRollingBack) {
      recurseForward(previousSchema, migration, databaseSchema)
    } else {
      recurseForRollback(previousSchema, migration, databaseSchema)
    }
  }

  def recurseForward(previousSchema: Schema, migration: Migration, databaseSchema: DatabaseSchema): Future[MigrationApplierResult] = {
    if (migration.pendingSteps.nonEmpty) {
      val result = for {
        _             <- applyStep(previousSchema, migration, migration.currentStep, databaseSchema)
        nextMigration = migration.incApplied
        _             <- migrationPersistence.updateMigrationApplied(migration.id, nextMigration.applied)
        x             <- recurseForward(previousSchema, nextMigration, databaseSchema)
      } yield x

      result.recoverWith {
        case exception =>
          println(s"Encountered exception while applying migration. Rolling back. $exception")
          exception.printStackTrace()

          for {
            _             <- migrationPersistence.updateMigrationStatus(migration.id, MigrationStatus.RollingBack)
            _             <- migrationPersistence.updateMigrationErrors(migration.id, migration.errors :+ StackTraceUtils.print(exception))
            applierResult <- recurseForRollback(previousSchema, migration.copy(status = MigrationStatus.RollingBack), databaseSchema)
          } yield applierResult
      }
    } else {
      migrationPersistence.updateMigrationStatus(migration.id, MigrationStatus.Success).map(_ => MigrationApplierResult(succeeded = true))
    }
  }

  def recurseForRollback(previousSchema: Schema, migration: Migration, databaseSchema: DatabaseSchema): Future[MigrationApplierResult] = {
    def continueRollback = {
      val nextMigration = migration.incRolledBack
      for {
        _ <- migrationPersistence.updateMigrationRolledBack(migration.id, nextMigration.rolledBack)
        x <- recurseForRollback(previousSchema, nextMigration, databaseSchema)
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
      unapplyStep(previousSchema, migration, migration.pendingRollBackSteps.head, databaseSchema).transformWith {
        case Success(_)   => continueRollback
        case Failure(err) => abortRollback(err)
      }
    } else {
      migrationPersistence.updateMigrationStatus(migration.id, MigrationStatus.RollbackSuccess).map(_ => MigrationApplierResult(succeeded = false))
    }
  }

  def applyStep(previousSchema: Schema, migration: Migration, step: MigrationStep, databaseSchema: DatabaseSchema): Future[Unit] = {
    migrationStepMapper.mutactionFor(previousSchema, migration.schema, step) match {
      case x if x.isEmpty =>
        Future.unit

      case list =>
        list.foldLeft(Future.unit) { (prev, mutaction) =>
          for {
            _ <- prev
            _ <- executeClientMutaction(mutaction, databaseSchema)
          } yield ()
        }
    }
  }

  def unapplyStep(previousSchema: Schema, migration: Migration, step: MigrationStep, databaseSchema: DatabaseSchema): Future[Unit] = {
    migrationStepMapper.mutactionFor(previousSchema, migration.schema, step) match {
      case x if x.isEmpty => Future.unit
      case list           => Future.sequence(list.map(executeClientMutactionRollback(_, databaseSchema))).map(_ => ())
    }
  }

  def executeClientMutaction(mutaction: DeployMutaction, tables: DatabaseSchema): Future[Unit] = mutactionExecutor.execute(mutaction, tables)

  def executeClientMutactionRollback(mutaction: DeployMutaction, tables: DatabaseSchema): Future[Unit] = mutactionExecutor.rollback(mutaction, tables)
}
