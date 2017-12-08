package cool.graph.deploy.migration
import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import cool.graph.deploy.database.persistence.MigrationPersistence
import cool.graph.shared.models.Migration
import slick.jdbc.MySQLProfile.backend.DatabaseDef

case class AsyncMigrator(clientDatabase: DatabaseDef, migrationPersistence: MigrationPersistence)(
    implicit val system: ActorSystem,
    materializer: ActorMaterializer
) extends Migrator {
  val job = system.actorOf(Props(MigrationApplierJob(clientDatabase, migrationPersistence)))

  override def schedule(migration: Migration): Unit = {}
}
