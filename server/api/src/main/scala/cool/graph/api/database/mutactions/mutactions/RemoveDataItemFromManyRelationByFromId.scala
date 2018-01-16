package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.shared.models.Field
import cool.graph.shared.models.IdType.Id

import scala.concurrent.Future

case class RemoveDataItemFromManyRelationByFromId(projectId: String, fromField: Field, fromId: Id) extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {
    val fromRelationSide = fromField.relationSide.get
    val relation         = fromField.relation.get

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteDataItemByValues(projectId, relation.id, Map(fromRelationSide.toString -> fromId))))
  }

  override def rollback: Some[Future[ClientSqlStatementResult[Any]]] = Some(ClientMutactionNoop().execute)
}
