package cool.graph.system.database

import cool.graph.GCDataTypes.GCStringConverter
import cool.graph.JsonFormats
import cool.graph.Types.Id
import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.models._
import spray.json._

object ModelToDbMapper {
  def convertProjectDatabase(projectDatabase: ProjectDatabase): cool.graph.system.database.tables.ProjectDatabase = {
    cool.graph.system.database.tables.ProjectDatabase(
      id = projectDatabase.id,
      region = projectDatabase.region,
      name = projectDatabase.name,
      isDefaultForRegion = projectDatabase.isDefaultForRegion
    )
  }

  def convertProject(project: Project): cool.graph.system.database.tables.Project = {
    cool.graph.system.database.tables.Project(
      id = project.id,
      alias = project.alias,
      name = project.name,
      revision = project.revision,
      webhookUrl = project.webhookUrl,
      clientId = project.ownerId,
      allowQueries = project.allowQueries,
      allowMutations = project.allowMutations,
      typePositions = project.typePositions,
      projectDatabaseId = project.projectDatabase.id,
      isEjected = project.isEjected,
      hasGlobalStarPermission = project.hasGlobalStarPermission
    )
  }

  def convertModel(project: Project, model: Model): cool.graph.system.database.tables.Model = {
    cool.graph.system.database.tables.Model(
      id = model.id,
      name = model.name,
      description = model.description,
      isSystem = model.isSystem,
      projectId = project.id,
      fieldPositions = model.fieldPositions
    )
  }

  def convertFunction(project: Project, function: Function): cool.graph.system.database.tables.Function = {
    function match {
      case ServerSideSubscriptionFunction(id, name, isActive, query, queryFilePath, delivery) =>
        val dbFunctionWithoutDelivery = cool.graph.system.database.tables.Function(
          id = id,
          projectId = project.id,
          name = name,
          binding = FunctionBinding.SERVERSIDE_SUBSCRIPTION,
          functionType = delivery.functionType,
          isActive = isActive,
          requestPipelineMutationModelId = None,
          requestPipelineMutationOperation = None,
          serversideSubscriptionQuery = Some(query),
          serversideSubscriptionQueryFilePath = queryFilePath,
          lambdaArn = None,
          webhookUrl = None,
          webhookHeaders = None,
          inlineCode = None,
          inlineCodeFilePath = None,
          auth0Id = None,
          schema = None,
          schemaFilePath = None
        )
        mergeDeliveryIntoDbFunction(delivery, dbFunctionWithoutDelivery)

      case RequestPipelineFunction(id, name, isActive, binding, modelId, operation, delivery) =>
        val dbFunctionWithoutDelivery = cool.graph.system.database.tables.Function(
          id = id,
          projectId = project.id,
          name = name,
          binding = binding,
          functionType = delivery.functionType,
          isActive = isActive,
          requestPipelineMutationModelId = Some(modelId),
          requestPipelineMutationOperation = Some(operation),
          serversideSubscriptionQuery = None,
          serversideSubscriptionQueryFilePath = None,
          lambdaArn = None,
          webhookUrl = None,
          webhookHeaders = None,
          inlineCode = None,
          inlineCodeFilePath = None,
          auth0Id = None,
          schema = None,
          schemaFilePath = None
        )
        mergeDeliveryIntoDbFunction(delivery, dbFunctionWithoutDelivery)

      case CustomMutationFunction(id, name, isActive, schema, schemaFilePath, delivery, _, _, _) =>
        val dbFunctionWithoutDelivery = cool.graph.system.database.tables.Function(
          id = id,
          projectId = project.id,
          name = name,
          binding = FunctionBinding.CUSTOM_MUTATION,
          functionType = delivery.functionType,
          isActive = isActive,
          requestPipelineMutationModelId = None,
          requestPipelineMutationOperation = None,
          serversideSubscriptionQuery = None,
          serversideSubscriptionQueryFilePath = None,
          lambdaArn = None,
          webhookUrl = None,
          webhookHeaders = None,
          inlineCode = None,
          inlineCodeFilePath = None,
          auth0Id = None,
          schema = Some(schema),
          schemaFilePath = schemaFilePath
        )
        mergeDeliveryIntoDbFunction(delivery, dbFunctionWithoutDelivery)

      case CustomQueryFunction(id, name, isActive, schema, schemaFilePath, delivery, _, _, _) =>
        val dbFunctionWithoutDelivery = cool.graph.system.database.tables.Function(
          id = id,
          projectId = project.id,
          name = name,
          binding = FunctionBinding.CUSTOM_QUERY,
          functionType = delivery.functionType,
          isActive = isActive,
          requestPipelineMutationModelId = None,
          requestPipelineMutationOperation = None,
          serversideSubscriptionQuery = None,
          serversideSubscriptionQueryFilePath = None,
          lambdaArn = None,
          webhookUrl = None,
          webhookHeaders = None,
          inlineCode = None,
          inlineCodeFilePath = None,
          auth0Id = None,
          schema = Some(schema),
          schemaFilePath = schemaFilePath
        )
        mergeDeliveryIntoDbFunction(delivery, dbFunctionWithoutDelivery)
    }
  }

  private def mergeDeliveryIntoDbFunction(delivery: FunctionDelivery,
                                          dbFunction: cool.graph.system.database.tables.Function): cool.graph.system.database.tables.Function = {
    delivery match {
      case fn: WebhookFunction =>
        dbFunction.copy(
          functionType = FunctionType.WEBHOOK,
          webhookUrl = Some(fn.url),
          webhookHeaders = Some(HttpFunctionHeaders.write(fn.headers).toString)
        )
      case fn: Auth0Function =>
        dbFunction.copy(
          functionType = FunctionType.CODE,
          webhookUrl = Some(fn.url),
          webhookHeaders = Some(HttpFunctionHeaders.write(fn.headers).toString),
          auth0Id = Some(fn.auth0Id),
          inlineCode = Some(fn.code),
          inlineCodeFilePath = fn.codeFilePath
        )
      case fn: ManagedFunction =>
        dbFunction.copy(
          functionType = FunctionType.CODE,
          inlineCodeFilePath = fn.codeFilePath
        )
//      case fn: LambdaFunction =>
//        dbFunction.copy(
//          functionType = FunctionType.LAMBDA,
//          inlineCode = Some(fn.code),
//          lambdaArn = Some(fn.arn)
//        )
    }
  }

  def convertField(modelId: Id, field: Field): cool.graph.system.database.tables.Field = {
    cool.graph.system.database.tables.Field(
      id = field.id,
      name = field.name,
      typeIdentifier = field.typeIdentifier.toString,
      description = field.description,
      isRequired = field.isRequired,
      isList = field.isList,
      isUnique = field.isUnique,
      isSystem = field.isSystem,
      isReadonly = field.isReadonly,
      defaultValue = field.defaultValue.flatMap(GCStringConverter(field.typeIdentifier, field.isList).fromGCValueToOptionalString),
      relationId = field.relation.map(_.id),
      relationSide = field.relationSide,
      modelId = modelId,
      enumId = field.enum.map(_.id)
    )
  }

  def convertFieldConstraint(constraint: FieldConstraint): cool.graph.system.database.tables.FieldConstraint = {
    implicit val anyFormat = JsonFormats.AnyJsonFormat
    constraint match {
      case string: StringConstraint =>
        cool.graph.system.database.tables.FieldConstraint(
          id = string.id,
          constraintType = string.constraintType,
          fieldId = string.fieldId,
          equalsString = string.equalsString,
          oneOfString = string.oneOfString.asInstanceOf[Any].toJson.compactPrint,
          minLength = string.minLength,
          maxLength = string.maxLength,
          startsWith = string.startsWith,
          endsWith = string.endsWith,
          includes = string.includes,
          regex = string.regex
        )
      case number: NumberConstraint =>
        cool.graph.system.database.tables.FieldConstraint(
          id = number.id,
          constraintType = number.constraintType,
          fieldId = number.fieldId,
          equalsNumber = number.equalsNumber,
          oneOfNumber = number.oneOfNumber.asInstanceOf[Any].toJson.compactPrint,
          min = number.min,
          max = number.max,
          exclusiveMin = number.exclusiveMin,
          exclusiveMax = number.exclusiveMax,
          multipleOf = number.multipleOf
        )

      case boolean: BooleanConstraint =>
        cool.graph.system.database.tables.FieldConstraint(
          id = boolean.id,
          constraintType = boolean.constraintType,
          fieldId = boolean.fieldId,
          equalsBoolean = boolean.equalsBoolean
        )

      case list: ListConstraint =>
        cool.graph.system.database.tables.FieldConstraint(id = list.id,
                                                          constraintType = list.constraintType,
                                                          fieldId = list.fieldId,
                                                          uniqueItems = list.uniqueItems,
                                                          minItems = list.minItems,
                                                          maxItems = list.maxItems)
    }
  }
}
