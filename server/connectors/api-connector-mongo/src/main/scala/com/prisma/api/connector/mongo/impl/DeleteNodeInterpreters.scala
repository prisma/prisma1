package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.database.{MongoAction, MongoActionsBuilder, SequenceAction}
import com.prisma.api.connector.mongo.{NestedDatabaseMutactionInterpreter, TopLevelDatabaseMutactionInterpreter}
import com.prisma.api.schema.APIErrors
import com.prisma.gc_values.IdGCValue

import scala.concurrent.ExecutionContext

case class DeleteNodeInterpreter(mutaction: TopLevelDeleteNode)(implicit val ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  override def mongoAction(mutationBuilder: MongoActionsBuilder) = {
    for {
      nodeOpt <- mutationBuilder.getNodeByWhere2(mutaction.where)
      node <- nodeOpt match {
               case Some(node) =>
                 for {
//            _ <- performCascadingDelete(mutationBuilder, mutaction.where.model, node.id)
                   _ <- checkForRequiredRelationsViolations(mutationBuilder, node.id)
                   _ <- mutationBuilder.deleteNodeById(mutaction.where.model, node.id)
                 } yield node
               case None =>
                 throw APIErrors.NodeNotFoundForWhereError(mutaction.where)
             }
    } yield MutactionResults(Vector(DeleteNodeResult(node.id, node, mutaction)))
  }

  private def checkForRequiredRelationsViolations(mutationBuilder: MongoActionsBuilder, id: IdGCValue) = {
    val fieldsWhereThisModelIsRequired = mutaction.model.schema.fieldsWhereThisModelIsRequired(mutaction.where.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodeIsInRelation(id, field)).toVector
    SequenceAction(actions)
  }
}

case class NestedDeleteNodeInterpreter(mutaction: NestedDeleteNode)(implicit ec: ExecutionContext) extends NestedDatabaseMutactionInterpreter {
  override def mongoAction(mutationBuilder: MongoActionsBuilder, parentId: IdGCValue): MongoAction[MutactionResults] = {
    mutationBuilder.nestedDeleteNode(mutaction, parentId)
  }
}
