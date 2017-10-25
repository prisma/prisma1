package cool.graph.client.mutactions

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.database.{DataResolver, DatabaseMutationBuilder}
import cool.graph.shared.errors.UserAPIErrors
import cool.graph.shared.models.{Field, Project}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

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
      val model = project.getModelByFieldId_!(field.id)
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
        case (_, false)   => Failure(UserAPIErrors.NodeNotFoundError(bId))
        case (false, _)   => Failure(UserAPIErrors.NodeNotFoundError(aId))
      }
    }
  }
}
