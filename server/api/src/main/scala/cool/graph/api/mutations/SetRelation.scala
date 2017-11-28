package cool.graph.api.mutations

import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataResolver
import cool.graph.api.database.mutactions.{MutactionGroup, Transaction}
import cool.graph.api.database.mutactions.mutactions.{AddDataItemToManyRelation, InvalidInput, RemoveDataItemFromRelationById}
import cool.graph.api.mutations.definitions.SetRelationDefinition
import cool.graph.api.schema.APIErrors.RelationIsRequired
import cool.graph.shared.models.IdType.Id
import cool.graph.shared.models._
import sangria.schema
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SetRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver)(
    implicit apiDependencies: ApiDependencies)
    extends ClientMutation(fromModel, args, dataResolver) {

  override val mutationDefinition = SetRelationDefinition(relation, project)

  val fromId: Id = extractIdFromScalarArgumentValues_!(args, mutationDefinition.bName)
  val toId: Id   = extractIdFromScalarArgumentValues_!(args, mutationDefinition.aName)

  def prepareMutactions(): Future[List[MutactionGroup]] = {

    val sqlMutactions = List(
      RemoveDataItemFromRelationById(project, relation.id, fromId),
      RemoveDataItemFromRelationById(project, relation.id, toId),
      AddDataItemToManyRelation(project, fromModel, relation.getModelAField_!(project), toId, fromId)
    )

    val field        = project.getModelById_!(fromModel.id).relationFields.find(_.relation.get == relation).get
    val relatedField = field.relatedFieldEager(project)
    val relatedModel = field.relatedModel_!(project)

    val checkFrom =
      InvalidInput(RelationIsRequired(fieldName = relatedField.name, typeName = relatedModel.name), requiredOneRelationCheck(field, relatedField, fromId, toId))

    val checkTo =
      InvalidInput(RelationIsRequired(fieldName = field.name, typeName = fromModel.name), requiredOneRelationCheck(relatedField, field, toId, fromId))

    val transactionMutaction = Transaction(sqlMutactions, dataResolver)

    Future.successful(
      List(
        MutactionGroup(mutactions = List(checkFrom, checkTo, transactionMutaction), async = false),
        // todo: dummy mutaction group for actions to satisfy tests. Please implement actions :-)
        MutactionGroup(mutactions = List(), async = true)
      ))
  }

  override def getReturnValue: Future[ReturnValueResult] = returnValueById(fromModel, fromId)

  def requiredOneRelationCheck(field: Field, relatedField: Field, fromId: String, toId: String): Future[Boolean] = {
    relatedField.isRequired && !relatedField.isList match {
      case true =>
        dataResolver.resolveByRelation(fromField = field, fromModelId = fromId, args = None).map { resolverResult =>
          val items = resolverResult.items
          items.isEmpty match {
            case true  => false
            case false => items.head.id != toId
          }
        }
      case false => Future.successful(false)
    }
  }

}
