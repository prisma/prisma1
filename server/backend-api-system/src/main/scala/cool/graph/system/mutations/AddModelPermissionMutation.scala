package cool.graph.system.mutations

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.PermissionQueryIsInvalid
import cool.graph.shared.models
import cool.graph.shared.models.{Model, Project}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.migration.permissions.QueryPermissionHelper
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateModelPermission, CreateModelPermissionField, InvalidateSchema}
import sangria.relay.Mutation
import scaldi.Injector

case class AddModelPermissionMutation(
    client: models.Client,
    project: models.Project,
    model: models.Model,
    args: AddModelPermissionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(
    implicit inj: Injector,
    actorSystem: ActorSystem
) extends InternalProjectMutation[AddModelPermissionMutationPayload] {

  //at the moment the console sends empty strings, these would cause problems for the rendering of the clientInterchange
  val ruleName: Option[String] = args.ruleName match {
    case Some("") => None
    case x        => x
  }

  var newModelPermission = models.ModelPermission(
    id = Cuid.createCuid(),
    operation = args.operation,
    userType = args.userType,
    rule = args.rule,
    ruleName = ruleName,
    ruleGraphQuery = args.ruleGraphQuery,
    ruleGraphQueryFilePath = args.ruleGraphQueryFilePath,
    ruleWebhookUrl = args.ruleWebhookUrl,
    fieldIds = args.fieldIds,
    applyToWholeModel = args.applyToWholeModel,
    description = args.description,
    isActive = args.isActive
  )

  val newModel: Model = model.copy(permissions = model.permissions :+ newModelPermission)

  val updatedProject: Project = project.copy(models = project.models.filter(_.id != newModel.id) :+ newModel)

  override def prepareActions(): List[Mutaction] = {

//    newModelPermission.ruleGraphQuery.foreach { query =>
//      val queriesWithSameOpCount = model.permissions.count(_.operation == newModelPermission.operation)
//
//      val queryName = newModelPermission.ruleName match {
//        case Some(nameForRule) => nameForRule
//        case None              => QueryPermissionHelper.alternativeNameFromOperationAndInt(newModelPermission.operationString, queriesWithSameOpCount)
//      }
//
//      val args         = QueryPermissionHelper.permissionQueryArgsFromModel(model)
//      val treatedQuery = QueryPermissionHelper.prependNameAndRenderQuery(query, queryName: String, args: List[(String, String)])
//
//      val violations = QueryPermissionHelper.validatePermissionQuery(treatedQuery, project)
//      if (violations.nonEmpty)
//        actions ++= List(InvalidInput(PermissionQueryIsInvalid(violations.mkString(""), newModelPermission.ruleName.getOrElse(newModelPermission.id))))
//    }

    actions :+= CreateModelPermission(project = project, model = model, permission = newModelPermission)

    actions ++= newModelPermission.fieldIds.map(fieldId => CreateModelPermissionField(project, model, newModelPermission, fieldId))

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[AddModelPermissionMutationPayload] = {
    Some(
      AddModelPermissionMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        model = newModel,
        modelPermission = newModelPermission
      ))
  }
}

case class AddModelPermissionMutationPayload(clientMutationId: Option[String],
                                             project: models.Project,
                                             model: models.Model,
                                             modelPermission: models.ModelPermission)
    extends Mutation

case class AddModelPermissionInput(clientMutationId: Option[String],
                                   modelId: String,
                                   operation: models.ModelOperation.Value,
                                   userType: models.UserType.Value,
                                   rule: models.CustomRule.Value,
                                   ruleName: Option[String],
                                   ruleGraphQuery: Option[String],
                                   ruleWebhookUrl: Option[String],
                                   fieldIds: List[String],
                                   applyToWholeModel: Boolean,
                                   description: Option[String],
                                   isActive: Boolean,
                                   ruleGraphQueryFilePath: Option[String] = None)
