package cool.graph.client.mutactions

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.shared.models.Field

import scala.concurrent.Future
import scala.util.Success

case class RemoveDataItemFromRelationByField(projectId: String, relationId: String, field: Field, id: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteRelationRowBySideAndId(projectId, relationId, field.relationSide.get, id)))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
