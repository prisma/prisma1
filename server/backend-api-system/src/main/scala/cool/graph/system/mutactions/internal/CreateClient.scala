package cool.graph.system.mutactions.internal

import java.sql.SQLIntegrityConstraintViolationException

import cool.graph.shared.errors.UserInputErrors.ClientEmailInUse
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.{ClientTable, RelayIdTable}
import cool.graph.shared.models.Client
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Success, Try}

case class CreateClient(client: Client) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {
    val clients  = TableQuery[ClientTable]
    val relayIds = TableQuery[RelayIdTable]

    Future.successful(
      SystemSqlStatementResult(
        sqlAction = DBIO.seq(
          clients += cool.graph.system.database.tables.Client(
            id = client.id,
            auth0Id = client.auth0Id,
            isAuth0IdentityProviderEmail = client.isAuth0IdentityProviderEmail,
            name = client.name,
            email = client.email,
            password = client.hashedPassword,
            resetPasswordToken = client.resetPasswordSecret,
            source = client.source,
            createdAt = DateTime.now(),
            updatedAt = DateTime.now()
          ),
          relayIds += cool.graph.system.database.tables
            .RelayId(client.id, "Client")
        )))
  }

  override def rollback = Some(DeleteClient(client).execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    // todo: check valid email, valid password
    // todo: make  email column in sql unique

    Future.successful(Success(MutactionVerificationSuccess()))
  }

  override def handleErrors =
    // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
    Some({ case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 => ClientEmailInUse() })
}
