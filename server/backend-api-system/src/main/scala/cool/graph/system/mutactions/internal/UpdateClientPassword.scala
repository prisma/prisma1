package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models.Client
import cool.graph.system.database.tables.ClientTable
import cool.graph.util.crypto.Crypto
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class UpdateClientPassword(client: Client, oldPassword: String, newPassword: String) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val hashedPassword = Crypto.hash(password = newPassword)

    val clients = TableQuery[ClientTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { c <- clients if c.id === client.id } yield (c.password)
      q.update(hashedPassword)
    })))
  }

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    if (!Crypto.verify(oldPassword, client.hashedPassword)) {
      Future.successful(Failure(UserInputErrors.InvalidPassword()))
    } else Future.successful(Success(MutactionVerificationSuccess()))
  }
}
