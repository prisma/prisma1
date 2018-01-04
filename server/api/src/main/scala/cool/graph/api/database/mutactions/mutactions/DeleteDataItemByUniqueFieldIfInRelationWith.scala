package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.NodeSelector
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project, Relation}

import scala.concurrent.Future

case class DeleteDataItemByUniqueFieldIfInRelationWith(
    project: Project,
    fromModel: Model,
    fromField: Field,
    fromId: Id,
    where: NodeSelector
) extends ClientSqlDataChangeMutaction {
  assert(
    fromModel.fields.exists(_.id == fromField.id),
    s"${fromModel.name} does not contain the field ${fromField.name}. If this assertion fires, this mutaction is used wrong by the programmer."
  )

  val relation: Relation      = fromField.relation.get
  val aModel: Model           = relation.getModelA_!(project.schema)
  val deleteByUniqueValueForB = aModel.name == fromModel.name

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (deleteByUniqueValueForB) {
      DatabaseMutationBuilder.deleteDataItemByUniqueValueForBIfInRelationWithGivenA(project.id, relation.id, fromId, where)
    } else {
      DatabaseMutationBuilder.deleteDataItemByUniqueValueForAIfInRelationWithGivenB(project.id, relation.id, fromId, where)
    }
    ClientSqlStatementResult(sqlAction = action)
  }
}
