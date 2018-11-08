package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SequenceAction}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue
import org.mongodb.scala.MongoWriteException

import scala.concurrent.ExecutionContext

case class DeleteNodesInterpreter(mutaction: DeleteNodes)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  def mongoAction(mutationBuilder: MongoActionsBuilder) =
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      _   <- checkForRequiredRelationsViolations(mutationBuilder, ids)
      _   <- mutationBuilder.deleteNodes(mutaction.model, ids)
    } yield MutactionResults(Vector(ManyNodesResult(mutaction, ids.size)))

  private def checkForRequiredRelationsViolations(mutationBuilder: MongoActionsBuilder, nodeIds: Seq[IdGCValue]) = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(nodeIds.toVector, field))

    SequenceAction(actions.toVector)
  }
}

case class UpdateNodesInterpreter(mutaction: UpdateNodes)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  def mongoAction(mutationBuilder: MongoActionsBuilder) =
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      _   <- mutationBuilder.updateNodes(mutaction, ids)
    } yield MutactionResults(Vector(ManyNodesResult(mutaction, ids.size)))

  override val errorMapper = {
    case e: MongoWriteException if e.getError.getCode == 11000 && MongoErrorMessageHelper.getFieldOption(mutaction.model, e).isDefined =>
      APIErrors.UniqueConstraintViolation(mutaction.model.name, MongoErrorMessageHelper.getFieldOption(mutaction.model, e).get)
  }
}

case class ResetDataInterpreter(mutaction: ResetData)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  def mongoAction(mutationBuilder: MongoActionsBuilder) = {
    mutationBuilder.truncateTables(mutaction)
  }
}
