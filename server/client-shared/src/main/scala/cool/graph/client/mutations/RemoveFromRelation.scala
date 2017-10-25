package cool.graph.client.mutations

import cool.graph.Types.Id
import cool.graph.client.authorization.RelationMutationPermissions
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.RemoveFromRelationDefinition
import cool.graph.shared.models._
import cool.graph.{_}
import sangria.schema
import scaldi._

import scala.concurrent.Future

class RemoveFromRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver, argumentSchema: ArgumentSchema)(
    implicit inj: Injector)
    extends ClientMutation(fromModel, args, dataResolver, argumentSchema) {

  override val mutationDefinition = RemoveFromRelationDefinition(relation, project, argumentSchema)

  var aId: Id = extractIdFromScalarArgumentValues_!(args, mutationDefinition.bName)

  def prepareMutactions(): Future[List[MutactionGroup]] = {

    val aField = relation.getModelAField_!(project)
    val bField = relation.getModelBField_!(project)

    val bId = extractIdFromScalarArgumentValues_!(args, mutationDefinition.aName)

    var sqlMutactions = List[ClientSqlMutaction]()

    sqlMutactions :+=
      RemoveDataItemFromRelationByToAndFromField(project = project, relationId = relation.id, aField = aField, aId = aId, bField = bField, bId = bId)

    // note: for relations between same model, same field we add a relation row for both directions
    if (aField == bField) {
      sqlMutactions :+=
        RemoveDataItemFromRelationByToAndFromField(project = project, relationId = relation.id, aField = bField, aId = bId, bField = aField, bId = aId)
    }

    val transactionMutaction = Transaction(sqlMutactions, dataResolver)

    Future.successful(
      List(
        MutactionGroup(mutactions = List(transactionMutaction), async = false),
        // dummy mutaction group for actions to satisfy tests. Please implement actions :-)
        MutactionGroup(mutactions = List(), async = true)
      ))
  }

  override def getReturnValue: Future[ReturnValueResult] = returnValueById(fromModel, aId)

  override def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit] = {
    RelationMutationPermissions.checkAllPermissions(project, mutactions, authenticatedRequest)
  }

  private def extractActions: List[Action] = {
    project.actions
      .filter(_.isActive)
      .filter(_.triggerMutationModel.exists(_.modelId == fromModel.id))
      .filter(_.triggerMutationModel.exists(_.mutationType == ActionTriggerMutationModelMutationType.Create))
  }
}
