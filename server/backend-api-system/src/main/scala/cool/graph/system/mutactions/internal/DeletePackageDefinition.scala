package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{PackageDefinition, Project}
import cool.graph.system.database.tables.{PackageDefinitionTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeletePackageDefinition(project: Project, packageDefinition: PackageDefinition, internalDatabase: DatabaseDef) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val packageDefinitions = TableQuery[PackageDefinitionTable]
    val relayIds           = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(packageDefinitions.filter(_.id === packageDefinition.id).delete, relayIds.filter(_.id === packageDefinition.id).delete)))
  }

  override def rollback = Some(CreatePackageDefinition(project, packageDefinition, internalDatabase).execute)

}
