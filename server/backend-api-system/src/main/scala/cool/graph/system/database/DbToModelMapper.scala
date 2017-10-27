package cool.graph.system.database

import cool.graph.GCDataTypes.GCStringConverter
import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.models
import cool.graph.shared.models.{FieldConstraintType, FunctionBinding, IntegrationName, NumberConstraint, StringConstraint}
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.database.tables._
import spray.json.DefaultJsonProtocol._
import spray.json._

case class AllDataForProject(
    project: Project,
    models: Seq[Model],
    fields: Seq[Field],
    relations: Seq[Relation],
    relationFieldMirrors: Seq[RelationFieldMirror],
    rootTokens: Seq[RootToken],
    actions: Seq[Action],
    actionTriggerMutationModels: Seq[ActionTriggerMutationModel],
    actionTriggerMutationRelations: Seq[ActionTriggerMutationRelation],
    actionHandlerWebhooks: Seq[ActionHandlerWebhook],
    integrations: Seq[Integration],
    modelPermissions: Seq[ModelPermission],
    modelPermissionFields: Seq[ModelPermissionField],
    relationPermissions: Seq[RelationPermission],
    auth0s: Seq[IntegrationAuth0],
    digits: Seq[IntegrationDigits],
    algolias: Seq[SearchProviderAlgolia],
    algoliaSyncQueries: Seq[AlgoliaSyncQuery],
    seats: Seq[(Seat, Option[Client])],
    packageDefinitions: Seq[PackageDefinition],
    enums: Seq[Enum],
    featureToggles: Seq[FeatureToggle],
    functions: Seq[Function],
    fieldConstraints: Seq[FieldConstraint],
    projectDatabase: ProjectDatabase
)

object DbToModelMapper {
  def createClient(client: Client) = {
    models.Client(
      id = client.id,
      auth0Id = client.auth0Id,
      isAuth0IdentityProviderEmail = client.isAuth0IdentityProviderEmail,
      name = client.name,
      email = client.email,
      hashedPassword = client.password,
      resetPasswordSecret = client.resetPasswordToken,
      source = client.source,
      projects = List.empty,
      createdAt = client.createdAt,
      updatedAt = client.updatedAt
    )
  }

  def createProject(allData: AllDataForProject): models.Project = {

    val projectModels = createModelList(allData).toList
    val project       = allData.project

    models.Project(
      id = project.id,
      ownerId = project.clientId,
      alias = project.alias,
      name = project.name,
      webhookUrl = project.webhookUrl,
      models = projectModels,
      relations = createRelationList(allData).toList,
      enums = createEnumList(allData),
      actions = createActionList(allData).toList,
      rootTokens = createRootTokenList(allData).toList,
      integrations = createIntegrationList(allData, projectModels).toList,
      seats = createSeatList(allData).toList,
      allowQueries = project.allowQueries,
      allowMutations = project.allowMutations,
      revision = project.revision,
      packageDefinitions = createPackageDefinitionList(allData).toList,
      featureToggles = createFeatureToggleList(allData),
      functions = createFunctionList(allData).toList,
      typePositions = project.typePositions.toList,
      projectDatabase = createProjectDatabase(allData.projectDatabase),
      isEjected = project.isEjected,
      hasGlobalStarPermission = project.hasGlobalStarPermission
    )
  }

  def createProjectDatabase(projectDatabase: ProjectDatabase): models.ProjectDatabase = {
    models.ProjectDatabase(
      id = projectDatabase.id,
      region = projectDatabase.region,
      name = projectDatabase.name,
      isDefaultForRegion = projectDatabase.isDefaultForRegion
    )
  }

  def createSeatList(allData: AllDataForProject) = {
    allData.seats.map { seat =>
      models.Seat(
        id = seat._1.id,
        status = seat._1.status,
        isOwner = seat._1.clientId.contains(allData.project.clientId),
        email = seat._1.email,
        clientId = seat._1.clientId,
        name = seat._2.map(_.name)
      )
    }
  }

  def createFunctionList(allData: AllDataForProject): Seq[models.Function] = {
    allData.functions
      .map { function =>
        val delivery = function.functionType match {
          case models.FunctionType.CODE if function.inlineCode.nonEmpty =>
            models.Auth0Function(
              code = function.inlineCode.get,
              codeFilePath = function.inlineCodeFilePath,
              auth0Id = function.auth0Id.get,
              url = function.webhookUrl.get,
              headers = HttpFunctionHeaders.read(function.webhookHeaders)
            )

          case models.FunctionType.CODE if function.inlineCode.isEmpty =>
            models.ManagedFunction()
//          case models.FunctionType.LAMBDA =>
//            models.LambdaFunction(
//              code = function.inlineCode.get,
//              arn = function.lambdaArn.get
//            )

          case models.FunctionType.WEBHOOK =>
            models.WebhookFunction(
              url = function.webhookUrl.get,
              headers = HttpFunctionHeaders.read(function.webhookHeaders)
            )
        }

        function.binding match {
          case FunctionBinding.SERVERSIDE_SUBSCRIPTION =>
            models.ServerSideSubscriptionFunction(
              id = function.id,
              name = function.name,
              isActive = function.isActive,
              query = function.serversideSubscriptionQuery.get,
              queryFilePath = function.serversideSubscriptionQueryFilePath,
              delivery = delivery
            )

          case FunctionBinding.TRANSFORM_PAYLOAD | FunctionBinding.TRANSFORM_ARGUMENT | FunctionBinding.PRE_WRITE | FunctionBinding.TRANSFORM_REQUEST |
              FunctionBinding.TRANSFORM_RESPONSE =>
            models.RequestPipelineFunction(
              id = function.id,
              name = function.name,
              isActive = function.isActive,
              binding = function.binding,
              modelId = function.requestPipelineMutationModelId.get,
              operation = function.requestPipelineMutationOperation.get,
              delivery = delivery
            )

          case FunctionBinding.CUSTOM_MUTATION =>
            models.CustomMutationFunction(
              id = function.id,
              name = function.name,
              isActive = function.isActive,
              schema = function.schema.get,
              schemaFilePath = function.schemaFilePath,
              delivery = delivery
            )

          case FunctionBinding.CUSTOM_QUERY =>
            models.CustomQueryFunction(
              id = function.id,
              name = function.name,
              isActive = function.isActive,
              schema = function.schema.get,
              schemaFilePath = function.schemaFilePath,
              delivery = delivery
            )
        }
      }
  }

  def createPackageDefinitionList(allData: AllDataForProject) = {
    allData.packageDefinitions.map { definition =>
      models.PackageDefinition(
        id = definition.id,
        name = definition.name,
        definition = definition.definition,
        formatVersion = definition.formatVersion
      )
    }
  }

  def createModelList(allData: AllDataForProject) = {
    allData.models.map { model =>
      models.Model(
        id = model.id,
        name = model.name,
        description = model.description,
        isSystem = model.isSystem,
        fields = createFieldList(model, allData).toList,
        permissions = createModelPermissionList(model, allData).toList,
        fieldPositions = model.fieldPositions.toList
      )
    }
  }

  def createFieldList(model: Model, allData: AllDataForProject) = {
    allData.fields
      .filter(_.modelId == model.id)
      .map { field =>
        val enum = for {
          enumId <- field.enumId
          enum   <- allData.enums.find(_.id == enumId)
        } yield createEnum(enum)

        val constraints = for {
          fieldConstraint <- allData.fieldConstraints.filter(_.fieldId == field.id)
        } yield createFieldConstraint(fieldConstraint)

        val typeIdentifier = CustomScalarTypes.parseTypeIdentifier(field.typeIdentifier)
        models.Field(
          id = field.id,
          name = field.name,
          typeIdentifier = typeIdentifier,
          description = field.description,
          isRequired = field.isRequired,
          isList = field.isList,
          isUnique = field.isUnique,
          isSystem = field.isSystem,
          isReadonly = field.isReadonly,
          defaultValue = field.defaultValue.map(GCStringConverter(typeIdentifier, field.isList).toGCValue(_).get),
          relation = field.relationId.map(id => createRelation(id, allData)),
          relationSide = field.relationSide,
          enum = enum,
          constraints = constraints.toList
        )
      }
  }

  def createRelationList(allData: AllDataForProject): Seq[models.Relation] = {
    allData.relations.map { relation =>
      createRelation(relation.id, allData)
    }
  }

  def createEnumList(allData: AllDataForProject): List[models.Enum] = {
    allData.enums.map(createEnum).toList
  }

  def createEnum(enum: Enum): models.Enum = {
    models.Enum(
      id = enum.id,
      name = enum.name,
      values = enum.values.parseJson.convertTo[List[String]]
    )
  }

  def createFieldConstraint(constraint: FieldConstraint): models.FieldConstraint = {
    constraint.constraintType match {
      case FieldConstraintType.STRING =>
        StringConstraint(
          id = constraint.id,
          fieldId = constraint.fieldId,
          equalsString = constraint.equalsString,
          oneOfString = constraint.oneOfString.parseJson.convertTo[List[String]],
          minLength = constraint.minLength,
          maxLength = constraint.maxLength,
          startsWith = constraint.startsWith,
          endsWith = constraint.endsWith,
          includes = constraint.includes,
          regex = constraint.regex
        )
      case FieldConstraintType.NUMBER =>
        NumberConstraint(
          id = constraint.id,
          fieldId = constraint.fieldId,
          equalsNumber = constraint.equalsNumber,
          oneOfNumber = constraint.oneOfNumber.parseJson.convertTo[List[Double]],
          min = constraint.min,
          max = constraint.max,
          exclusiveMin = constraint.exclusiveMin,
          exclusiveMax = constraint.exclusiveMax,
          multipleOf = constraint.multipleOf
        )

      case FieldConstraintType.BOOLEAN =>
        models.BooleanConstraint(id = constraint.id, fieldId = constraint.fieldId, equalsBoolean = constraint.equalsBoolean)

      case FieldConstraintType.LIST =>
        models.ListConstraint(id = constraint.id,
                              fieldId = constraint.fieldId,
                              uniqueItems = constraint.uniqueItems,
                              minItems = constraint.minItems,
                              maxItems = constraint.maxItems)
    }
  }

  def createFeatureToggleList(allData: AllDataForProject): List[models.FeatureToggle] = {
    allData.featureToggles.map { featureToggle =>
      models.FeatureToggle(
        id = featureToggle.id,
        name = featureToggle.name,
        isEnabled = featureToggle.isEnabled
      )
    }.toList
  }

  def createRelation(relationId: String, allData: AllDataForProject) = {
    val relation = allData.relations.find(_.id == relationId).get

    models.Relation(
      id = relation.id,
      name = relation.name,
      description = relation.description,
      modelAId = relation.modelAId,
      modelBId = relation.modelBId,
      fieldMirrors = createFieldMirrorList(relation, allData).toList,
      permissions = createRelationPermissionList(relation, allData).toList
    )
  }

  def createFieldMirrorList(relation: Relation, allData: AllDataForProject): Seq[models.RelationFieldMirror] = {
    allData.relationFieldMirrors
      .filter(_.relationId == relation.id)
      .map { fieldMirror =>
        models.RelationFieldMirror(
          id = fieldMirror.id,
          relationId = fieldMirror.relationId,
          fieldId = fieldMirror.fieldId
        )
      }
  }

  def createModelPermissionList(model: Model, allData: AllDataForProject) = {
    allData.modelPermissions
      .filter(_.modelId == model.id)
      .map(permission => {
        models.ModelPermission(
          id = permission.id,
          operation = permission.operation,
          userType = permission.userType,
          rule = permission.rule,
          ruleName = permission.ruleName,
          ruleGraphQuery = permission.ruleGraphQuery,
          ruleGraphQueryFilePath = permission.ruleGraphQueryFilePath,
          ruleWebhookUrl = permission.ruleWebhookUrl,
          fieldIds = allData.modelPermissionFields
            .filter(_.modelPermissionId == permission.id)
            .toList
            .map(_.fieldId)
            .distinct,
          applyToWholeModel = permission.applyToWholeModel,
          isActive = permission.isActive,
          description = permission.description
        )
      })
  }

  def createRelationPermissionList(relation: Relation, allData: AllDataForProject) = {
    allData.relationPermissions
      .filter(_.relationId == relation.id)
      .map(permission => {

        models.RelationPermission(
          id = permission.id,
          connect = permission.connect,
          disconnect = permission.disconnect,
          userType = permission.userType,
          rule = permission.rule,
          ruleName = permission.ruleName,
          ruleGraphQuery = permission.ruleGraphQuery,
          ruleGraphQueryFilePath = permission.ruleGraphQueryFilePath,
          ruleWebhookUrl = permission.ruleWebhookUrl,
          isActive = permission.isActive
        )
      })
  }

  def createActionList(allData: AllDataForProject) = {
    allData.actions.map { action =>
      val handlerWebhook = allData.actionHandlerWebhooks
        .find(_.actionId == action.id)
        .map { wh =>
          models.ActionHandlerWebhook(id = wh.id, url = wh.url, isAsync = wh.isAsync)
        }

      val triggerModel = allData.actionTriggerMutationModels
        .find(_.actionId == action.id)
        .map { m =>
          models.ActionTriggerMutationModel(
            id = m.id,
            modelId = m.modelId,
            mutationType = m.mutationType,
            fragment = m.fragment
          )
        }

      val triggerRelation = allData.actionTriggerMutationRelations
        .find(_.actionId == action.id)
        .map { m =>
          models.ActionTriggerMutationRelation(
            id = m.id,
            relationId = m.relationId,
            mutationType = m.mutationType,
            fragment = m.fragment
          )
        }

      models.Action(
        id = action.id,
        isActive = action.isActive,
        triggerType = action.triggerType,
        handlerType = action.handlerType,
        description = action.description,
        handlerWebhook = handlerWebhook,
        triggerMutationModel = triggerModel,
        triggerMutationRelation = triggerRelation
      )
    }
  }

  def createRootTokenList(allData: AllDataForProject) = {
    allData.rootTokens.map { token =>
      models.RootToken(
        id = token.id,
        token = token.token,
        name = token.name,
        created = token.created
      )
    }
  }

  def createIntegrationList(allData: AllDataForProject, projectModels: List[models.Model]): Seq[models.Integration] = {
    allData.integrations
      .map { integration =>
        integration.name match {
          case IntegrationName.AuthProviderAuth0 =>
            val meta =
              allData.auth0s
                .find(_.integrationId == integration.id)
                .map(auth0 => models.AuthProviderAuth0(id = auth0.id, domain = auth0.domain, clientId = auth0.clientId, clientSecret = auth0.clientSecret))

            models.AuthProvider(
              id = integration.id,
              subTableId = meta.map(_.id).getOrElse(""),
              isEnabled = integration.isEnabled,
              name = integration.name,
              metaInformation = meta
            )

          case IntegrationName.AuthProviderDigits =>
            val meta =
              allData.digits
                .find(_.integrationId == integration.id)
                .map(digits => models.AuthProviderDigits(id = digits.id, consumerKey = digits.consumerKey, consumerSecret = digits.consumerSecret))

            models.AuthProvider(
              id = integration.id,
              subTableId = meta.map(_.id).getOrElse(""),
              isEnabled = integration.isEnabled,
              name = integration.name,
              metaInformation = meta
            )

          case IntegrationName.AuthProviderEmail =>
            models.AuthProvider(
              id = integration.id,
              subTableId = "",
              isEnabled = integration.isEnabled,
              name = integration.name,
              metaInformation = None
            )

          case IntegrationName.SearchProviderAlgolia =>
            val algolia = allData.algolias.find(_.integrationId == integration.id).get
            val syncQueries = allData.algoliaSyncQueries
              .filter(_.searchProviderAlgoliaId == algolia.id)
              .map { syncQuery =>
                models.AlgoliaSyncQuery(
                  id = syncQuery.id,
                  indexName = syncQuery.indexName,
                  fragment = syncQuery.query,
                  isEnabled = syncQuery.isEnabled,
                  model = projectModels.find(_.id == syncQuery.modelId).get
                )
              }

            models.SearchProviderAlgolia(
              id = integration.id,
              subTableId = algolia.id,
              applicationId = algolia.applicationId,
              apiKey = algolia.apiKey,
              algoliaSyncQueries = syncQueries.toList,
              isEnabled = integration.isEnabled,
              name = integration.name
            )
        }
      }
  }
}
