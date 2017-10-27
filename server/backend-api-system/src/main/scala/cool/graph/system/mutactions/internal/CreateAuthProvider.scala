package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.cuid.Cuid
import cool.graph.system.database.tables.{IntegrationAuth0 => _, IntegrationDigits => _, Project => _, _}
import cool.graph.shared.models._
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Success, Try}

case class CreateAuthProvider(project: Project, name: IntegrationName.Value, metaInformation: Option[AuthProviderMetaInformation], isEnabled: Boolean)
    extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val integrations = TableQuery[IntegrationTable]
    val digitsTable  = TableQuery[IntegrationDigitsTable]
    val auth0Table   = TableQuery[IntegrationAuth0Table]
    val relayIds     = TableQuery[RelayIdTable]

    val id = Cuid.createCuid()

    val addIntegration = List(
      integrations += cool.graph.system.database.tables
        .Integration(id = id, isEnabled = isEnabled, integrationType = IntegrationType.AuthProvider, name = name, projectId = project.id),
      relayIds += cool.graph.system.database.tables.RelayId(id, "Integration")
    )

    val addMeta = metaInformation match {
      case Some(digits: AuthProviderDigits) if digits.isInstanceOf[AuthProviderDigits] => {
        List(
          digitsTable += cool.graph.system.database.tables.IntegrationDigits(
            id = Cuid.createCuid(),
            integrationId = id,
            consumerKey = digits.consumerKey,
            consumerSecret = digits.consumerSecret
          ))
      }
      case Some(auth0: AuthProviderAuth0) if auth0.isInstanceOf[AuthProviderAuth0] => {
        List(
          auth0Table += cool.graph.system.database.tables.IntegrationAuth0(
            id = Cuid.createCuid(),
            integrationId = id,
            clientId = auth0.clientId,
            clientSecret = auth0.clientSecret,
            domain = auth0.domain
          ))
      }
      case _ => List()
    }

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(addIntegration ++ addMeta: _*)
      ))
  }
}
