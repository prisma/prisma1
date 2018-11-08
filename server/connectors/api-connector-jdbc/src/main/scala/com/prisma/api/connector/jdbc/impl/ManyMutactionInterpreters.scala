package com.prisma.api.connector.jdbc.impl

import com.prisma.api.connector._
import com.prisma.api.connector.jdbc.TopLevelDatabaseMutactionInterpreter
import com.prisma.api.connector.jdbc.database.JdbcActionsBuilder
import com.prisma.gc_values.IdGCValue
import slick.dbio._

import scala.concurrent.ExecutionContext

object ParameterLimit {
  //Postgres has a limit of 32767 parameters but when updating scalar lists we set three parameters per item in the group
  val groupSize = 10000
}

case class DeleteNodesInterpreter(mutaction: DeleteNodes, shouldDeleteRelayIds: Boolean)(implicit ec: ExecutionContext)
    extends TopLevelDatabaseMutactionInterpreter {

  def dbioAction(mutationBuilder: JdbcActionsBuilder) =
    for {
      ids        <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      groupedIds = ids.grouped(ParameterLimit.groupSize).toVector
      _          <- DBIO.seq(groupedIds.map(checkForRequiredRelationsViolations(mutationBuilder, _)): _*)
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.deleteNodes(mutaction.model, _, shouldDeleteRelayIds)): _*)
    } yield ManyNodesResult(mutaction, ids.size)

  private def checkForRequiredRelationsViolations(mutationBuilder: JdbcActionsBuilder, nodeIds: Vector[IdGCValue]): DBIO[_] = {
    val fieldsWhereThisModelIsRequired = mutaction.project.schema.fieldsWhereThisModelIsRequired(mutaction.model)
    val actions                        = fieldsWhereThisModelIsRequired.map(field => mutationBuilder.errorIfNodesAreInRelation(nodeIds, field))

    DBIO.sequence(actions)
  }
}

case class UpdateNodesInterpreter(mutaction: UpdateNodes)(implicit ec: ExecutionContext) extends TopLevelDatabaseMutactionInterpreter {

  def dbioAction(mutationBuilder: JdbcActionsBuilder) =
    for {
      ids        <- mutationBuilder.getNodeIdsByFilter(mutaction.model, mutaction.whereFilter)
      groupedIds = ids.grouped(ParameterLimit.groupSize).toVector
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.updateNodesByIds(mutaction.model, mutaction.nonListArgs, _)): _*)
      _          <- DBIO.seq(groupedIds.map(mutationBuilder.updateScalarListValuesForIds(mutaction.model, mutaction.listArgs, _)): _*)
    } yield ManyNodesResult(mutaction, ids.size)
}

case class ResetDataInterpreter(mutaction: ResetData) extends TopLevelDatabaseMutactionInterpreter {
  def dbioAction(mutationBuilder: JdbcActionsBuilder) = {
    mutationBuilder.truncateTables(mutaction.project).andThen(unitResult)
  }
}
