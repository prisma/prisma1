package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{PackageDefinitionTable, RelayIdTable}
import slick.jdbc.MySQLProfile.api._
import slick.jdbc.MySQLProfile.backend.DatabaseDef
import slick.lifted.TableQuery

import scala.concurrent.Future

case class CreatePackageDefinition(project: Project,
                                   packageDefinition: PackageDefinition,
                                   internalDatabase: DatabaseDef,
                                   ignoreDuplicateNameVerificationError: Boolean = false)
    extends SystemSqlMutaction {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val packageDefinitions = TableQuery[PackageDefinitionTable]
    val relayIds           = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO
          .seq(
            packageDefinitions += cool.graph.system.database.tables.PackageDefinition(
              id = packageDefinition.id,
              name = packageDefinition.name,
              definition = packageDefinition.definition,
              formatVersion = packageDefinition.formatVersion,
              projectId = project.id
            ),
            relayIds +=
              cool.graph.system.database.tables.RelayId(packageDefinition.id, "PackageDefinition")
          )
      ))
  }

  override def rollback = Some(DeletePackageDefinition(project, packageDefinition, internalDatabase).execute)

}
