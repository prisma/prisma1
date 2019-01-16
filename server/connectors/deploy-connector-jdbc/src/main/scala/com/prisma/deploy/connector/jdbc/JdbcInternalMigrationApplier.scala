package com.prisma.deploy.connector.jdbc
import com.prisma.connector.shared.jdbc.SlickDatabase
import com.prisma.deploy.connector.InternalMigrationApplier
import com.prisma.deploy.connector.persistence.InternalMigration
import com.prisma.shared.models.Project
import slick.dbio.DBIO
import org.jooq.impl.DSL._

import scala.concurrent.{ExecutionContext, Future}

case class JdbcInternalMigrationApplier(slickDatabase: SlickDatabase)(implicit ec: ExecutionContext) extends JdbcBase with InternalMigrationApplier {

  def apply(internalMigration: InternalMigration, project: Project): Future[Unit] = {
    val action = internalMigration match {
      case InternalMigration.RemoveIdColumnFromRelationTables =>
        removeIdColumnFromRelationTables(project)
    }
    slickDatabase.database.run(action)
  }

  def removeIdColumnFromRelationTables(project: Project): DBIO[Unit] = {
    val actions = for {
      relation <- project.relations
      if !relation.hasManifestation
    } yield {
      val query = sql
        .alterTable(table(name(project.id, relation.relationTableName)))
        .drop(field("id"))

      changeDatabaseQueryToDBIO(query)()
    }
    DBIO.seq(actions: _*)
  }
}
