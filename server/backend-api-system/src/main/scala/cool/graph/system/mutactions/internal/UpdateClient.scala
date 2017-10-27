package cool.graph.system.mutactions.internal

import java.sql.SQLIntegrityConstraintViolationException

import com.github.tototoshi.slick.MySQLJodaSupport._
import cool.graph._
import cool.graph.shared.errors.UserInputErrors.ClientEmailInUse
import cool.graph.shared.models.Client
import cool.graph.system.database.tables.ClientTable
import org.joda.time.DateTime
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class UpdateClient(oldClient: Client, client: Client) extends SystemSqlMutaction {
  override def execute: Future[SystemSqlStatementResult[Any]] = {

    val clients = TableQuery[ClientTable]

    Future.successful(SystemSqlStatementResult(sqlAction = DBIO.seq({
      val q = for { c <- clients if c.id === client.id } yield (c.name, c.email, c.updatedAt)
      q.update((client.name, client.email, DateTime.now()))
    })))
  }

  override def rollback: Some[Future[SystemSqlStatementResult[Any]]] = Some(UpdateClient(oldClient, oldClient).execute)

  override def handleErrors =
    Some({
      // https://dev.mysql.com/doc/refman/5.5/en/error-messages-server.html#error_er_dup_entry
      case e: SQLIntegrityConstraintViolationException if e.getErrorCode == 1062 => ClientEmailInUse()
    })
}
