package cool.graph.shared.models

import cool.graph.shared.ApiMatrixFactory
import scaldi.{Injectable, Injector}

// returns Models, fields etc from a project taking ApiMatrix into account
object ModelParser extends Injectable {

  def action(project: Project, actionId: String): Option[Action] = {
    project.actions.find(_.id == actionId)
  }

  def relation(project: Project, relationId: String, injector: Injector): Option[Relation] = {
    val apiMatrix = getApiMatrixFactory(injector).create(project)
    apiMatrix.filterRelations(project.relations).find(_.id == relationId)
  }

  def seat(project: Project, seatId: String): Option[Seat] = {
    project.seats.find(_.id == seatId)
  }

  def packageDefinition(project: Project, packageDefinitionId: String): Option[PackageDefinition] = {
    project.packageDefinitions.find(_.id == packageDefinitionId)
  }

  def relationByName(project: Project, relationName: String, injector: Injector): Option[Relation] = {
    val apiMatrix = getApiMatrixFactory(injector).create(project)
    project.relations.find(relation => relation.name == relationName && apiMatrix.includeRelation(relation))
  }

  def actionTriggerMutationModel(project: Project, actionTriggerMutationModelId: String): Option[ActionTriggerMutationModel] = {
    project.actions
      .flatMap(_.triggerMutationModel)
      .find(_.id == actionTriggerMutationModelId)
  }

  def actionTriggerMutationRelation(project: Project, actionTriggerMutationRelationId: String): Option[ActionTriggerMutationRelation] = {
    project.actions
      .flatMap(_.triggerMutationRelation)
      .find(_.id == actionTriggerMutationRelationId)
  }

  def actionHandlerWebhook(project: Project, actionHandlerWebhookId: String): Option[ActionHandlerWebhook] = {
    project.actions
      .flatMap(_.handlerWebhook)
      .find(_.id == actionHandlerWebhookId)
  }

  def function(project: Project, functionId: String): Option[Function] = {
    project.functions.find(_.id == functionId)
  }

  def modelPermission(project: Project, modelPermissionId: String): Option[ModelPermission] = {
    project.models
      .flatMap(_.permissions)
      .find(_.id == modelPermissionId)
  }

  def relationPermission(project: Project, relationPermissionId: String, injector: Injector): Option[RelationPermission] = {
    val apiMatrix = getApiMatrixFactory(injector).create(project)
    apiMatrix
      .filterRelations(project.relations)
      .flatMap(_.permissions)
      .find(_.id == relationPermissionId)
  }

  def integration(
      project: Project,
      integrationId: String
  ): Option[Integration] = {
    project.integrations
      .find(_.id == integrationId)
  }

  def algoliaSyncQuery(
      project: Project,
      algoliaSyncQueryId: String
  ): Option[AlgoliaSyncQuery] = {
    project.integrations
      .collect {
        case x: SearchProviderAlgolia =>
          x
      }
      .flatMap(_.algoliaSyncQueries)
      .find(_.id == algoliaSyncQueryId)
  }

  def field(project: Project, fieldId: String, injector: Injector): Option[Field] = {
    val apiMatrix = getApiMatrixFactory(injector).create(project)
    apiMatrix
      .filterModels(project.models)
      .flatMap(model => apiMatrix.filterFields(model.fields))
      .find(_.id == fieldId)
  }

  def fieldByName(project: Project, modelName: String, fieldName: String, injector: Injector): Option[Field] = {
    val apiMatrix = getApiMatrixFactory(injector).create(project)
    apiMatrix
      .filterModels(project.models)
      .find(_.name == modelName)
      .map(model => apiMatrix.filterFields(model.fields))
      .flatMap(_.find(_.name == fieldName))

  }

  def model(project: Project, modelId: String, injector: Injector): Option[Model] = {
    val apiMatrix = getApiMatrixFactory(injector).create(project)
    apiMatrix.filterModels(project.models).find(_.id == modelId)
  }

  def modelByName(project: Project, modelName: String, injector: Injector): Option[Model] = {

    val apiMatrix = getApiMatrixFactory(injector).create(project)
    project.models.find(
      model =>
        model.name == modelName &&
          apiMatrix.includeModel(model.name))

  }

  private def getApiMatrixFactory(injector: Injector): ApiMatrixFactory = {
    implicit val inj = injector
    inject[ApiMatrixFactory]
  }
}
