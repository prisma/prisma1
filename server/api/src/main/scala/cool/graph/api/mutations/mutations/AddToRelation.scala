package cool.graph.api.mutations.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.mutactions.{AddDataItemToManyRelation, RemoveDataItemFromRelationByField}
import cool.graph.api.database.mutactions.{ClientSqlMutaction, Mutaction, MutactionGroup, Transaction}
import cool.graph.api.mutations.definitions.AddToRelationDefinition
import cool.graph.api.mutations.{ClientMutation, ReturnValueResult}
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models._
import sangria.schema
import scaldi._

import scala.concurrent.Future

class AddToRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver)(
    implicit apiDependencies: ApiDependencies)
    extends ClientMutation(fromModel, args, dataResolver) {

  override val mutationDefinition = AddToRelationDefinition(relation, project)

  var fromId: Id = extractIdFromScalarArgumentValues_!(args, mutationDefinition.bName)

  val aField: Option[Field] = relation.getModelAField(project)
  val bField: Option[Field] = relation.getModelBField(project)

  def prepareMutactions(): Future[List[MutactionGroup]] = {
    val toId = extractIdFromScalarArgumentValues_!(args, mutationDefinition.aName)

    var sqlMutactions = List[ClientSqlMutaction]()

    if (aField.isDefined && !aField.get.isList) {
      sqlMutactions :+= RemoveDataItemFromRelationByField(project.id, relation.id, aField.get, fromId)
    }

    if (bField.isDefined && !bField.get.isList) {
      sqlMutactions :+= RemoveDataItemFromRelationByField(project.id, relation.id, bField.get, toId)
    }

    sqlMutactions :+= AddDataItemToManyRelation(project, fromModel, relation.getModelAField_!(project), toId, fromId)

    // note: for relations between same model, same field we add a relation row for both directions
    if (aField == bField) {
      sqlMutactions :+= AddDataItemToManyRelation(project, fromModel, relation.getModelAField_!(project), fromId, toId)
    }

    val transactionMutaction = Transaction(sqlMutactions, dataResolver)
    Future.successful(
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false),
        // dummy mutaction group for actions to satisfy tests. Please implement actions :-)
        MutactionGroup(mutactions = List(), async = true)
      ))
  }

  override def getReturnValue: Future[ReturnValueResult] = returnValueById(fromModel, fromId)
}
