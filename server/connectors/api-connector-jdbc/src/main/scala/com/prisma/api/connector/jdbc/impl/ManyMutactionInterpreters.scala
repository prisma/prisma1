package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.DatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.gc_values.IdGCValue
import slick.dbio.DBIOAction
import slick.dbio._

import scala.concurrent.ExecutionContext

case class DeleteDataItemsInterpreter(mutaction: DeleteNodes)(implicit ec: ExecutionContext) extends DatabaseMutactionInterpreter {
  def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) =
    for {
      ids <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      _   <- checkForRequiredRelationsViolations(mutationBuilder, ids)
      _   <- mutationBuilder.deleteNodes(mutaction.model, ids)
    } yield UnitDatabaseMutactionResult

  private def checkForRequiredRelationsViolations(mutationBuilder: JdbcActionsBuilder, nodeIds: Vector[IdGCValue]): DBIO[_] = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(nodeIds, field))

    DBIO.sequence(actions)
  }
}

case class ResetDataInterpreter(mutaction: ResetData) extends DatabaseMutactionInterpreter {
  def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    mutationBuilder.truncateTables(mutaction.project).andThen(unitResult)
  }
}

case class UpdateDataItemsInterpreter(mutaction: UpdateNodes) extends DatabaseMutactionInterpreter {
  def dbioAction(mutationBuilder: JdbcActionsBuilder, parentId: IdGCValue) = {
    val nonListActions = mutationBuilder.updateNodes(mutaction.model, mutaction.updateArgs, mutaction.whereFilter)
    val listActions    = mutationBuilder.setScalarListValuesByFilter(mutaction.model, mutaction.listArgs, mutaction.whereFilter)
    DBIOAction.seq(listActions, nonListActions).andThen(unitResult)
  }
}
