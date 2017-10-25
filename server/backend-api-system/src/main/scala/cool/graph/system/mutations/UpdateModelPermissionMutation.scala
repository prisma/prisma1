package cool.graph.system.mutations

import _root_.akka.actor.ActorSystem
import _root_.akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.system.mutactions.internal._
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateModelPermissionMutation(
    client: models.Client,
    project: models.Project,
    model: models.Model,
    modelPermission: models.ModelPermission,
    args: UpdateModelPermissionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector, actorSystem: ActorSystem)
    extends InternalProjectMutation[UpdateModelPermissionMutationPayload] {

  val updatedModelPermission = models.ModelPermission(
    id = modelPermission.id,
    operation = args.operation.getOrElse(modelPermission.operation),
    userType = args.userType.getOrElse(modelPermission.userType),
    rule = args.rule.getOrElse(modelPermission.rule),
    ruleName = args.ruleName match {
      case None => modelPermission.ruleName
      case x    => x
    },
    ruleGraphQuery = args.ruleGraphQuery match {
      case None => modelPermission.ruleGraphQuery
      case x    => x
    },
    ruleGraphQueryFilePath = args.ruleGraphQueryFilePath match {
      case None => modelPermission.ruleGraphQueryFilePath
      case x    => x
    },
    ruleWebhookUrl = args.ruleWebhookUrl match {
      case None => modelPermission.ruleWebhookUrl
      case x    => x
    },
    fieldIds = args.fieldIds.getOrElse(modelPermission.fieldIds),
    applyToWholeModel = args.applyToWholeModel.getOrElse(modelPermission.applyToWholeModel),
    description = args.description match {
      case None => modelPermission.description
      case x    => x
    },
    isActive = args.isActive.getOrElse(modelPermission.isActive)
  )

  override def prepareActions(): List[Mutaction] = {

//    updatedModelPermission.ruleGraphQuery.foreach { query =>
//      val queriesWithSameOpCount = model.permissions.count(_.operation == updatedModelPermission.operation) // Todo this count may be wrong
//
//      val queryName = updatedModelPermission.ruleName match {
//        case Some(nameForRule) => nameForRule
//        case None              => QueryPermissionHelper.alternativeNameFromOperationAndInt(updatedModelPermission.operationString, queriesWithSameOpCount)
//      }
//
//      val args         = QueryPermissionHelper.permissionQueryArgsFromModel(model)
//      val treatedQuery = QueryPermissionHelper.prependNameAndRenderQuery(query, queryName: String, args: List[(String, String)])
//
//      val violations = QueryPermissionHelper.validatePermissionQuery(treatedQuery, project)
//      if (violations.nonEmpty)
//        actions ++= List(InvalidInput(PermissionQueryIsInvalid(violations.mkString(""), updatedModelPermission.ruleName.getOrElse(updatedModelPermission.id))))
//    }

    actions :+= UpdateModelPermission(model = model, oldPermisison = modelPermission, permission = updatedModelPermission)

    val addPermissionFields    = updatedModelPermission.fieldIds.filter(id => !modelPermission.fieldIds.contains(id))
    val removePermissionFields = modelPermission.fieldIds.filter(id => !updatedModelPermission.fieldIds.contains(id))

    actions ++= addPermissionFields.map(fieldId => CreateModelPermissionField(project, model, updatedModelPermission, fieldId))

    actions ++= removePermissionFields.map(fieldId => DeleteModelPermissionField(project, model, updatedModelPermission, fieldId))

    actions :+= BumpProjectRevision(project = project)

    actions :+= InvalidateSchema(project = project)

    actions
  }

  override def getReturnValue: Option[UpdateModelPermissionMutationPayload] = {
    Some(
      UpdateModelPermissionMutationPayload(
        clientMutationId = args.clientMutationId,
        project = project,
        model = model.copy(permissions = model.permissions :+ updatedModelPermission),
        modelPermission = updatedModelPermission
      ))
  }
}

case class UpdateModelPermissionMutationPayload(clientMutationId: Option[String],
                                                project: models.Project,
                                                model: models.Model,
                                                modelPermission: models.ModelPermission)
    extends Mutation

case class UpdateModelPermissionInput(clientMutationId: Option[String],
                                      id: String,
                                      operation: Option[models.ModelOperation.Value],
                                      userType: Option[models.UserType.Value],
                                      rule: Option[models.CustomRule.Value],
                                      ruleName: Option[String],
                                      ruleGraphQuery: Option[String],
                                      ruleWebhookUrl: Option[String],
                                      fieldIds: Option[List[String]],
                                      applyToWholeModel: Option[Boolean],
                                      description: Option[String],
                                      isActive: Option[Boolean],
                                      ruleGraphQueryFilePath: Option[String] = None)
