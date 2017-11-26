package cool.graph.deploy.migration.mutactions

import cool.graph.deploy.database.DatabaseMutationBuilder
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.Future

case class DeleteModelTable(projectId: String, model: String) extends ClientSqlMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DBIO.seq(DatabaseMutationBuilder.dropTable(projectId = projectId, tableName = model))))
  }

  override def rollback = Some(CreateModelTable(projectId, model).execute)
}
