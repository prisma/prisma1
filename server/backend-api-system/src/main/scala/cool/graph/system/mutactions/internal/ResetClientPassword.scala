package cool.graph.system.mutactions.internal

import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.system.database.tables.ClientTable
import cool.graph.shared.models.Client
import cool.graph.util.crypto.Crypto
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future
import scala.util.{Success, Try}

case class ResetClientPassword(client: Client, resetPasswordToken: String, newPassword: String) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val hashedPassword = Crypto.hash(password = newPassword)

    val clients = TableQuery[ClientTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for {
        c <- clients if c.id === client.id &&
          c.resetPasswordToken === resetPasswordToken
      } yield (c.password, c.resetPasswordToken)
      q.update(hashedPassword, None)
    })))
  }

  override def rollback = Some(SystemMutactionNoop().execute)

  override def verify(): Future[Try[MutactionVerificationSuccess]] = {
    // todo: verify new password is valid (long / strong)
    Future.successful(Success(MutactionVerificationSuccess()))
  }
}
