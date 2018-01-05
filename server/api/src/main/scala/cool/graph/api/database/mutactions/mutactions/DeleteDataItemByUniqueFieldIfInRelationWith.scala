package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.{NodeSelector, ParentInfo}
import cool.graph.shared.models.{Model, Project}

import scala.concurrent.Future

case class DeleteDataItemByUniqueFieldIfInRelationWith(
    project: Project,
    parentInfo: ParentInfo,
    where: NodeSelector
) extends ClientSqlDataChangeMutaction {

  val aModel: Model           = parentInfo.relation.getModelA_!(project.schema)
  val deleteByUniqueValueForB = aModel.name == parentInfo.model.name

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (deleteByUniqueValueForB) {
      DatabaseMutationBuilder.deleteDataItemByUniqueValueForBIfInRelationWithGivenA(project.id, parentInfo, where)
    } else {
      DatabaseMutationBuilder.deleteDataItemByUniqueValueForAIfInRelationWithGivenB(project.id, parentInfo, where)
    }
    ClientSqlStatementResult(sqlAction = action)
  }

}
