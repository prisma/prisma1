package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.errors.UserInputErrors
import cool.graph.system.database.tables.SearchProviderAlgoliaTable
import cool.graph.shared.models.SearchProviderAlgolia
import cool.graph.system.externalServices.AlgoliaKeyChecker
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    val algoliaKeyChecker = inject[AlgoliaKeyChecker](identified by "algoliaKeyChecker")

    algoliaKeyChecker
      .verifyAlgoliaCredentialValidity(newSearchProviderAlgolia.applicationId, newSearchProviderAlgolia.apiKey)
      .map {
        case true  => Success(MutactionVerificationSuccess())
        case false => Failure(UserInputErrors.AlgoliaCredentialsDontHaveRequiredPermissions())
      }
  }
}
