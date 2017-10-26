package cool.graph.client.mutactions

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.shared.models.Project

import scala.concurrent.Future
import scala.util.Success

case class RemoveDataItemFromRelationById(project: Project, relationId: String, id: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(ClientSqlStatementResult(sqlAction = DatabaseMutationBuilder.deleteRelationRowById(project.id, relationId, id)))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
