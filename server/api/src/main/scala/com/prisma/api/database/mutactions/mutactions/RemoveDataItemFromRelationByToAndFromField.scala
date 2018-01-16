package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.api.database.mutactions.{ClientMutactionNoop, ClientSqlDataChangeMutaction, ClientSqlStatementResult, MutactionVerificationSuccess}
import cool.graph.api.schema.APIErrors
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Project}

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

case class RemoveDataItemFromRelationByToAndFromField(project: Project, relationId: String, aField: Field, aId: Id, bField: Field, bId: Id)
    extends ClientSqlDataChangeMutaction {

  override def execute: Future[ClientSqlStatementResult[Any]] = {

    val aRelationSide = aField.relationSide.get
    // note: for relations between same model, same field a and b relation side is the same, so
    // to handle that case we take oppositeRelationSide instead of bField.relationSide
    val bRelationSide = aField.oppositeRelationSide.get

    Future.successful(
      ClientSqlStatementResult(
        sqlAction = DatabaseMutationBuilder
          .deleteRelationRowByToAndFromSideAndId(project.id, relationId, aRelationSide, aId, bRelationSide, bId)))
  }

  override def rollback = Some(ClientMutactionNoop().execute)

  override def verify(resolver: DataResolver): Future[Try[MutactionVerificationSuccess] with Product with Serializable] = {
    def dataItemExists(field: Field, id: Id): Future[Boolean] = {
      val model = project.schema.getModelByFieldId_!(field.id)
      resolver.existsByModelAndId(model, id)
    }
    val dataItemAExists = dataItemExists(aField, aId)
    val dataItemBExists = dataItemExists(bField, bId)
    for {
      aExists <- dataItemAExists
      bExists <- dataItemBExists
    } yield {
      (aExists, bExists) match {
        case (true, true) => Success(MutactionVerificationSuccess())
        case (_, false)   => Failure(APIErrors.NodeNotFoundError(bId))
        case (false, _)   => Failure(APIErrors.NodeNotFoundError(aId))
      }
    }
  }
}
