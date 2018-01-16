package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.shared.models.Field
import cool.graph.shared.models.IdType.Id

import scala.concurrent.Future

case class RemoveDataItemFromRelationByField(projectId: String, relationId: String, field: Field, id: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteRelationRowBySideAndId(projectId, relationId, field.relationSide.get, id)))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
