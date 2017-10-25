package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.{Project, SearchProviderAlgolia}
import cool.graph.system.database.tables.{RelayIdTable, SearchProviderAlgoliaTable}
import scaldi.Injector
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteSearchProviderAlgolia(project: Project, integrationAlgolia: SearchProviderAlgolia)(implicit inj: Injector) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val integrationAlgolias = TableQuery[SearchProviderAlgoliaTable]
    val relayIds            = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(integrationAlgolias.filter(_.id === integrationAlgolia.id).delete, relayIds.filter(_.id === integrationAlgolia.id).delete)))
  }

  override def rollback = Some(CreateSearchProviderAlgolia(project, integrationAlgolia).execute)

}
