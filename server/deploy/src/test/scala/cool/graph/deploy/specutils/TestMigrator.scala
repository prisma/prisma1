package cool.graph.deploy.specutils

import akka.actor.ActorSystem
import cool.graph.deploy.database.persistence.{DbToModelMapper, MigrationPersistence}
import cool.graph.deploy.database.tables.ProjectTable
import cool.graph.deploy.migration.MigrationApplierImpl
import cool.graph.deploy.migration.migrator.Migrator
import cool.graph.shared.models._
import cool.graph.utils.await.AwaitUtils
import cool.graph.utils.future.FutureUtils.FutureOpt
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
  val applier = MigrationApplierImpl(clientDatabase)

  // For tests, the schedule directly does all the migration work
  override def schedule(nextProject: Project, steps: Vector[MigrationStep]): Future[Migration] = {
    val unappliedMigration: UnappliedMigration = (for {
      savedMigration                  <- migrationPersistence.create(nextProject, Migration(nextProject, steps))
      previousProjectWithMigrationOpt <- FutureOpt(internalDb.run(ProjectTable.byIdWithMigration(savedMigration.projectId))).future
      previousProjectWithMigration    = previousProjectWithMigrationOpt.getOrElse(sys.error(s"Can't find project ${nextProject.id} with applied migration"))
      previousProject                 = DbToModelMapper.convert(previousProjectWithMigration._1, previousProjectWithMigration._2)
    } yield {

      UnappliedMigration(previousProject, nextProject, savedMigration)
    }).await

    applier.applyMigration(unappliedMigration.previousProject, unappliedMigration.nextProject, unappliedMigration.migration).flatMap { result =>
      if (result.succeeded) {
        migrationPersistence.markMigrationAsApplied(unappliedMigration.migration).map { _ =>
          unappliedMigration.migration.copy(status = MigrationStatus.Success)
        }
      } else {
        Future.failed(new Exception("applyMigration resulted in an error"))
      }
    }
  }
}
