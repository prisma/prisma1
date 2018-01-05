package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
<<<<<<< HEAD
import cool.graph.api.mutations.{CoolArgs, NodeSelector, ParentInfo}
import cool.graph.shared.models.{Model, Project}
=======
import cool.graph.api.mutations.{CoolArgs, NodeSelector}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project, Relation}
import slick.dbio.DBIOAction
>>>>>>> graphql-database

import scala.concurrent.Future

case class UpdateDataItemByUniqueFieldIfInRelationWith(project: Project, parentInfo: ParentInfo, where: NodeSelector, args: CoolArgs)
    extends ClientSqlDataChangeMutaction {

  val aModel: Model           = parentInfo.relation.getModelA_!(project)
  val updateByUniqueValueForB = aModel.name == parentInfo.model.name
  val scalarArgs              = args.nonListScalarArgumentsAsCoolArgs(where.model)

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (updateByUniqueValueForB) {
      DatabaseMutationBuilder.updateDataItemByUniqueValueForBIfInRelationWithGivenA(project.id, parentInfo, where, scalarArgs.raw)
    } else {
      DatabaseMutationBuilder.updateDataItemByUniqueValueForAIfInRelationWithGivenB(project.id, parentInfo, where, scalarArgs.raw)
    }

    if (scalarArgs.isNonEmpty) {
      ClientSqlStatementResult(sqlAction = action)
    } else {
      ClientSqlStatementResult(sqlAction = DBIOAction.successful(()))
    }
  }
}
