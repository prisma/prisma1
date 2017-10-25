package cool.graph.client.mutations

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.authorization.RelationMutationPermissions
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.RemoveFromRelationDefinition
import cool.graph.shared.models._
import sangria.schema
import scaldi._

import scala.concurrent.Future

class UnsetRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver, argumentSchema: ArgumentSchema)(
    implicit inj: Injector)
    extends ClientMutation(fromModel, args, dataResolver, argumentSchema) {

  override val mutationDefinition = RemoveFromRelationDefinition(relation, project, argumentSchema)

  val aId: Id = extractIdFromScalarArgumentValues_!(args, mutationDefinition.bName)

  def prepareMutactions(): Future[List[MutactionGroup]] = {

    val aField = relation.getModelAField_!(project)
    val bField = relation.getModelBField_!(project)

    val bId = extractIdFromScalarArgumentValues_!(args, mutationDefinition.aName)

    val sqlMutactions = List(RemoveDataItemFromRelationByToAndFromField(project, relation.id, aField, aId, bField, bId))
//
//    val sqlMutactions = List(RemoveDataItemFromRelationById(project, relation.id, aId),
//                             RemoveDataItemFromRelationById(project, relation.id, bId))

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
}
