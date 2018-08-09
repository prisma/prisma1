package com.prisma.api.connector.mongo.impl

import com.prisma.api.connector._
import com.prisma.api.connector.mongo.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.mongo.database.{MongoActionsBuilder, SimpleMongoAction}

import scala.concurrent.ExecutionContext
//
//case class DeleteNodesInterpreter(mutaction: DeleteNodes, shouldDeleteRelayIds: Boolean)(implicit ec: ExecutionContext)
//    extends TopLevelDatabaseMutactionInterpreter {
//
//  def dbioAction(mutationBuilder: JdbcActionsBuilder) =
//    for {
//      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
//      _   <- checkForRequiredRelationsViolations(mutationBuilder, ids)
//      _   <- mutationBuilder.deleteNodes(mutaction.model, ids, shouldDeleteRelayIds)
//    } yield UnitDatabaseMutactionResult
//
//  private def checkForRequiredRelationsViolations(mutationBuilder: JdbcActionsBuilder, nodeIds: Vector[IdGCValue]): DBIO[_] = {
//    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.model)
//    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(nodeIds, field))
//
//    DBIO.sequence(actions)
//  }
//}

case class ResetDataInterpreter(mutaction: ResetData)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {
  def mongoAction(mutationBuilder: MongoActionsBuilder): SimpleMongoAction[UnitDatabaseMutactionResult.type] = {
    mutationBuilder.truncateTables(mutaction)
  }
}

//case class UpdateNodesInterpreter(mutaction: UpdateNodes) extends TopLevelDatabaseMutactionInterpreter {
//  def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
//    val nonListActions = mutationBuilder.updateNodes(mutaction.model, mutaction.updateArgs, mutaction.whereFilter)
//    val listActions    = mutationBuilder.setScalarListValuesByFilter(mutaction.model, mutaction.listArgs, mutaction.whereFilter)
//    DBIOAction.seq(listActions, nonListActions).andThen(unitResult)
//  }
//}
