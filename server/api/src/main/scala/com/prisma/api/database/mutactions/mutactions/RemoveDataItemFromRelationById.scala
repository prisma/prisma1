package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.Project

import scala.concurrent.Future

case class RemoveDataItemFromRelationById(project: Project, relationId: String, id: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteRelationRowById(project.id, relationId, id)))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
