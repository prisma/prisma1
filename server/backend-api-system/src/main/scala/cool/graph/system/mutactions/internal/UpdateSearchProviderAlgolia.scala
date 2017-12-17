package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models.SearchProviderAlgolia
import cool.graph.system.database.tables.SearchProviderAlgoliaTable
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateSearchProviderAlgolia(oldSearchProviderAlgolia: SearchProviderAlgolia, newSearchProviderAlgolia: SearchProviderAlgolia)(implicit inj: Injector)
    extends SystemSqlMutaction
    with Injectable {

  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val searchProviderTableAlgolias = TableQuery[SearchProviderAlgoliaTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { s <- searchProviderTableAlgolias if s.id === newSearchProviderAlgolia.subTableId } yield (s.applicationId, s.apiKey)
      q.update(newSearchProviderAlgolia.applicationId, newSearchProviderAlgolia.apiKey)
    })))
  }

  override def rollback = Some(UpdateSearchProviderAlgolia(oldSearchProviderAlgolia, oldSearchProviderAlgolia).execute)
}
