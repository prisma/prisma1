package cool.graph.client.mutactions

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.shared.models.Field

import scala.concurrent.Future
import scala.util.{Success, Try}

case class RemoveDataItemFromManyRelationByToId(projectId: String, fromField: Field, toId: Id) extends ClientSqlDataChangeMutaction {

  override def execute = {
    val toRelationSide = fromField.oppositeRelationSide.get
    val relation       = fromField.relation.get

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteDataItemByValues(projectId, relation.id, Map(toRelationSide.toString -> toId))))
  }

  override def rollback = {
    Some(ClientMutactionNoop().execute)
  }

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess]] = {

    // note: we intentionally don't require that a relation actually exists

    Future.successful(Success(MutactionVerificationSuccess()))
  }
}
