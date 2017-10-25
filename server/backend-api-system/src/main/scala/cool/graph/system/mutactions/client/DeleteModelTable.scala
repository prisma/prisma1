package cool.graph.system.mutactions.client

import cool.graph._
import cool.graph.client.database.{DatabaseMutationBuilder, ProjectRelayIdTable}
import cool.graph.shared.models.Model
import slick.jdbc.MySQLProfile.api._
import slick.lifted.TableQuery

import scala.concurrent.Future

case class DeleteModelTable(projectId: String, model: Model) extends ClientSqlSchemaChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val relayIds = TableQuery(new ProjectRelayIdTable(_, projectId))

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DBIO.seq(DatabaseMutationBuilder.dropTable(projectId = projectId, tableName = model.name), relayIds.filter(_.modelId === model.id).delete)))
  }

  override def rollback = Some(CreateModelTable(projectId, model).execute)
}
