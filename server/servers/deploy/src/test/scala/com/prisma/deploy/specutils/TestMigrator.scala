package com.prisma.deploy.specutils

import akka.actor.ActorSystem
import com.prisma.deploy.connector.persistence.MigrationPersistence
import com.prisma.deploy.connector.{DatabaseInspector, DeployMutactionExecutor, MigrationStepMapperImpl}
import com.prisma.deploy.migration.migrator.{MigrationApplierImpl, Migrator}
import com.prisma.shared.models._
import com.prisma.utils.await.AwaitUtils

import scala.concurrent.Future

case class TestMigrator(
    migrationPersistence: MigrationPersistence,
    mutactionExecutor: DeployMutactionExecutor,
    databaseInspector: DatabaseInspector
)(implicit val system: ActorSystem)
    extends Migrator
    with AwaitUtils {
  import system.dispatcher

  // For tests, the schedule directly does all the migration work to remove the asynchronous component
  override def schedule(
      projectId: String,
      nextSchema: Schema,
      steps: Vector[MigrationStep],
      functions: Vector[Function],
      rawDataModel: String
  ): Future[Migration] = {
    val stepMapper = MigrationStepMapperImpl(projectId)
    val applier    = MigrationApplierImpl(migrationPersistence, stepMapper, mutactionExecutor, databaseInspector)

    val result: Future[Migration] = for {
      savedMigration <- migrationPersistence.create(Migration(projectId, nextSchema, steps, functions, rawDataModel))
      lastMigration  <- migrationPersistence.getLastMigration(projectId)
      applied <- applier.apply(lastMigration.get.schema, savedMigration).flatMap { result =>
                  if (result.succeeded) {
                    migrationPersistence.updateMigrationStatus(savedMigration.id, MigrationStatus.Success).map { _ =>
                      savedMigration.copy(status = MigrationStatus.Success)
                    }
                  } else {

                    Future.failed(new Exception("Fatal: apply resulted in an error"))
                  }
                }
    } yield {
      applied
    }

    result.await(10)
    result
  }

  override def initialize: Unit = {}
}
