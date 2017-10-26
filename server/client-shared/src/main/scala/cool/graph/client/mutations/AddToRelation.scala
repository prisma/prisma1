package cool.graph.client.mutations

import cool.graph.Types.Id
import cool.graph._
import cool.graph.client.authorization.RelationMutationPermissions
import cool.graph.client.database.DataResolver
import cool.graph.client.mutactions._
import cool.graph.client.mutations.definitions.AddToRelationDefinition
import cool.graph.shared.models._
import sangria.schema
import scaldi._

import scala.concurrent.Future

class AddToRelation(relation: Relation, fromModel: Model, project: Project, args: schema.Args, dataResolver: DataResolver, argumentSchema: ArgumentSchema)(
    implicit inj: Injector)
    extends ClientMutation(fromModel, args, dataResolver, argumentSchema) {

  override val mutationDefinition = AddToRelationDefinition(relation, project, argumentSchema)

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

  override def checkPermissionsAfterPreparingMutactions(authenticatedRequest: Option[AuthenticatedRequest], mutactions: List[Mutaction]): Future[Unit] = {
    RelationMutationPermissions.checkAllPermissions(project, mutactions, authenticatedRequest)
  }
}
