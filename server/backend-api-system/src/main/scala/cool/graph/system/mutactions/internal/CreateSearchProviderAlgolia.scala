package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.errors.UserInputErrors
import cool.graph.system.database.tables.{RelayIdTable, SearchProviderAlgoliaTable}
import cool.graph.shared.models._
import cool.graph.system.externalServices.AlgoliaKeyChecker
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class CreateSearchProviderAlgolia(project: Project, searchProviderAlgolia: SearchProviderAlgolia)(implicit inj: Injector)
    extends SystemSqlMutaction
    with Injectable {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    Future.successful({
      val searchProviderAlgolias = TableQuery[SearchProviderAlgoliaTable]
      val relayIds               = TableQuery[RelayIdTable]
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          searchProviderAlgolias +=
            cool.graph.system.database.tables.SearchProviderAlgolia(searchProviderAlgolia.subTableId,
                                                                    searchProviderAlgolia.id,
                                                                    searchProviderAlgolia.applicationId,
                                                                    searchProviderAlgolia.apiKey),
          relayIds +=
            cool.graph.system.database.tables.RelayId(searchProviderAlgolia.subTableId, "SearchProviderAlgolia")
        ))
    })
  }

  override def rollback = Some(DeleteSearchProviderAlgolia(project, searchProviderAlgolia).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    project.integrations
      .collect {
        case existingSearchProviderAlgolias: SearchProviderAlgolia =>
          existingSearchProviderAlgolias
      }
      .foreach(spa => {
        if (spa.id != searchProviderAlgolia) // This comparison will always evaluate to true. Which results in the intended outcome but was probably not intentional. Leaving this in since there is no test coverage and the code will be removed soon.
          return Future.successful(Failure(UserInputErrors.ProjectAlreadyHasSearchProviderAlgolia()))
      })

    if (searchProviderAlgolia.applicationId.isEmpty && searchProviderAlgolia.apiKey.isEmpty) {
      Future.successful(Success(MutactionVerificationSuccess()))
    } else {
      val algoliaKeyChecker = inject[AlgoliaKeyChecker](identified by "algoliaKeyChecker")

      algoliaKeyChecker
        .verifyAlgoliaCredentialValidity(searchProviderAlgolia.applicationId, searchProviderAlgolia.apiKey)
        .map {
          case true  => Success(MutactionVerificationSuccess())
          case false => Failure(UserInputErrors.AlgoliaCredentialsDontHaveRequiredPermissions())
        }
    }
  }
}
