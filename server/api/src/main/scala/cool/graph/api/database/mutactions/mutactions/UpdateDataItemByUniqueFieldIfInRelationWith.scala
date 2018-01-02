package cool.graph.api.database.mutactions.mutactions

import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.api.database.mutactions.{ClientSqlDataChangeMutaction, ClientSqlStatementResult}
import cool.graph.api.mutations.{CoolArgs, NodeSelector}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models.{Field, Model, Project, Relation}

import scala.concurrent.Future

case class UpdateDataItemByUniqueFieldIfInRelationWith(
    project: Project,
    fromModel: Model,
    fromField: Field,
    fromId: Id,
    where: NodeSelector,
    args: CoolArgs
) extends ClientSqlDataChangeMutaction {
  assert(
    fromModel.fields.exists(_.id == fromField.id),
    s"${fromModel.name} does not contain the field ${fromField.name}. If this assertion fires, this mutaction is used wrong by the programmer."
  )

  val relation: Relation      = fromField.relation.get
  val aModel: Model           = relation.getModelA_!(project)
  val updateByUniqueValueForB = aModel.name == fromModel.name

  override def execute: Future[ClientSqlStatementResult[Any]] = Future.successful {
    val action = if (updateByUniqueValueForB) {
      DatabaseMutationBuilder.updateDataItemByUniqueValueForBIfInRelationWithGivenA(project.id, relation.id, fromId, where, args.raw)
    } else {
      DatabaseMutationBuilder.updateDataItemByUniqueValueForAIfInRelationWithGivenB(project.id, relation.id, fromId, where, args.raw)
    }
    ClientSqlStatementResult(sqlAction = action)
  }
}
