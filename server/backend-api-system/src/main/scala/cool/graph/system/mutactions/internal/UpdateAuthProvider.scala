package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.shared.models._
import cool.graph.system.database.tables.{IntegrationAuth0Table, IntegrationDigitsTable, IntegrationTable}
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateAuthProvider(project: Project,
                              authProvider: AuthProvider,
                              metaInformation: Option[AuthProviderMetaInformation] = None,
                              oldMetaInformationId: Option[String] = None)
    extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val authProviders     = TableQuery[IntegrationTable]
    val integrationDigits = TableQuery[IntegrationDigitsTable]
    val integrationAuth0s = TableQuery[IntegrationAuth0Table]

    val updateIntegration = {
      val q = for { a <- authProviders if a.id === authProvider.id } yield (a.isEnabled)
      q.update(authProvider.isEnabled)
    }

    val upsertIntegrationMeta = metaInformation match {
      case Some(digits: AuthProviderDigits) if digits.isInstanceOf[AuthProviderDigits] => {
        List(
          integrationDigits.insertOrUpdate(
            cool.graph.system.database.tables.IntegrationDigits(id = oldMetaInformationId.getOrElse(digits.id),
                                                                integrationId = authProvider.id,
                                                                consumerKey = digits.consumerKey,
                                                                consumerSecret = digits.consumerSecret)))
      }
      case Some(auth0: AuthProviderAuth0) if auth0.isInstanceOf[AuthProviderAuth0] => {
        List(
          integrationAuth0s.insertOrUpdate(
            cool.graph.system.database.tables.IntegrationAuth0(id = oldMetaInformationId.getOrElse(auth0.id),
                                                               integrationId = authProvider.id,
                                                               clientId = auth0.clientId,
                                                               clientSecret = auth0.clientSecret,
                                                               domain = auth0.domain)))
      }
      case _ => List()
    }

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq(List(updateIntegration) ++ upsertIntegrationMeta: _*)))
  }
}
