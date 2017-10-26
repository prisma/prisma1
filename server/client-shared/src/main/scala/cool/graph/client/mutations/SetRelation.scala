package cool.graph.client.mutations

import cool.graph.Types.Id
import cool.graph.shared.errors.UserAPIErrors.RelationIsRequired
import cool.graph._
import cool.graph.client.authorization.RelationMutationPermissions
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.SetRelationDefinition
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import sangria.schema
import scaldi._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class SetRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver, argumentSchema: ArgumentSchema)(
    implicit inj: Injector)
    extends ClientMutation(fromModel, args, dataResolver, argumentSchema) {

  override val mutationDefinition = SetRelationDefinition(relation, project, argumentSchema)

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

  override def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit] = {
    RelationMutationPermissions.checkAllPermissions(project, mutactions, authenticatedRequest)
  }

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
