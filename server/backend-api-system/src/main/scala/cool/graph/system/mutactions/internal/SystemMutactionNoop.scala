package cool.graph.system.mutactions.internal

import cool.graph._
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class SystemMutactionNoop() extends SystemSqlMutaction {

  override def execute = Future.successful(SystemSqlStatementResult(sqlAction = DBIO.successful(None)))

  override def rollback = Some(Future.successful(SystemSqlStatementResult(sqlAction = DBIO.successful(None))))

}
