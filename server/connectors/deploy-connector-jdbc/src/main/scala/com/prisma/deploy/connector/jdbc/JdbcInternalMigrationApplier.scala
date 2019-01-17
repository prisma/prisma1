package com.prisma.deploy.connector.jdbc
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.{DatabaseInspector, InternalMigrationApplier, Tables}
import com.prisma.deploy.connector.persistence.InternalMigration
import com.prisma.shared.models.Project
import slick.dbio.DBIO
import org.jooq.impl.DSL._

import scala.concurrent.{ExecutionContext, Future}

case class JdbcInternalMigrationApplier(
    slickDatabase: SlickDatabase,
    databaseInspector: DatabaseInspector
)(implicit ec: ExecutionContext)
    extends JdbcBase
    with InternalMigrationApplier {

  def apply(internalMigration: InternalMigration, project: Project): Future[Unit] = {
    internalMigration match {
      case InternalMigration.RemoveIdColumnFromRelationTables =>
        for {
          databaseSchema <- databaseInspector.inspect(project.id)
          _              <- slickDatabase.database.run(removeIdColumnFromRelationTables(project, databaseSchema))
        } yield ()

    }
  }

  def removeIdColumnFromRelationTables(project: Project, databaseSchema: Tables): DBIO[Unit] = {
    val actions = for {
      relation <- project.relations
      if !relation.hasManifestation
    } yield {
      databaseSchema.table(relation.relationTableName).flatMap(_.column("id")) match {
        case Some(idColumn) =>
          val query = sql
            .alterTable(table(name(project.id, relation.relationTableName)))
            .drop(field(idColumn.name))

          changeDatabaseQueryToDBIO(query)()

        case None =>
          DBIO.successful(())
      }
    }
    DBIO.seq(actions: _*).withPinnedSession
  }
}
