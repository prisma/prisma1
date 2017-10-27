package cool.graph.system.migration

import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.errors.SystemErrors.ProjectPushError
import cool.graph.shared.functions.{DeployFailure, DeployResponse, ExternalFile, FunctionEnvironment}
import cool.graph.shared.errors.SystemErrors.{ProjectPushError, SchemaError}
import cool.graph.shared.errors.UserInputErrors.SchemaExtensionParseError
import cool.graph.shared.functions._
import cool.graph.shared.functions.{DeployFailure, DeployResponse, ExternalFile, FunctionEnvironment, _}
import cool.graph.shared.models._
import cool.graph.system.externalServices.{Auth0Extend, Auth0FunctionData}
import cool.graph.system.migration.ProjectConfig.Ast.Permission
import cool.graph.system.migration.ProjectConfig.{Ast, AstPermissionWithAllInfos, FunctionWithFiles}
import cool.graph.system.migration.functions.FunctionDiff
import cool.graph.system.migration.permissions.PermissionDiff
import cool.graph.system.migration.permissions.QueryPermissionHelper._
import cool.graph.system.migration.rootTokens.RootTokenDiff
import cool.graph.system.mutations._
import scaldi.{Injectable, Injector}
import spray.json.{JsObject, JsString}

import scala.collection.Seq
import scala.concurrent.Await
import scala.concurrent.duration.Duration

object ModuleMigrator {
  def apply(client: Client,
            project: Project,
            parsedModules: Seq[ProjectConfig.Ast.Module],
            files: Map[String, String],
            externalFiles: Option[Map[String, ExternalFile]],
            afterSchemaMigration: Boolean = false,
            isDryRun: Boolean)(implicit inj: Injector): ModuleMigrator = {
    val oldModule = ProjectConfig.moduleFromProject(project)

    val schemas: Seq[String] = parsedModules.map(module => module.types.map(x => files.getOrElse(x, sys.error("path to types not correct"))).getOrElse("")) // todo: this is ugly
    val combinedSchema       = schemas.mkString(" ")

    val newPermissions: Vector[Permission] = parsedModules.flatMap(_.permissions).toVector

    val newPermissionsWithQueryFile: Vector[AstPermissionWithAllInfos] = newPermissions.map(permission => {
      astPermissionWithAllInfosFromAstPermission(permission, files)
    })

    val newFunctionsMapList: Seq[Map[String, ProjectConfig.Ast.Function]] = parsedModules.map(_.functions)
    val combinedFunctionsList: Map[String, ProjectConfig.Ast.Function] =
      newFunctionsMapList.foldLeft(Map.empty: Map[String, ProjectConfig.Ast.Function])(_ ++ _)

    val newRootTokens: Vector[String] = parsedModules.flatMap(_.rootTokens).toVector

    val functionDiff: FunctionDiff     = FunctionDiff(project, oldModule.module, combinedFunctionsList, files)
    val permissionDiff: PermissionDiff = PermissionDiff(project, newPermissionsWithQueryFile, files, afterSchemaMigration)
    val rootTokenDiff: RootTokenDiff   = RootTokenDiff(project, newRootTokens)

    ModuleMigrator(functionDiff, permissionDiff, rootTokenDiff, client, project, files, externalFiles, combinedSchema, isDryRun)
  }
}

case class ModuleMigrator(functionDiff: FunctionDiff,
                          permissionDiff: PermissionDiff,
                          rootTokenDiff: RootTokenDiff,
                          client: Client,
                          project: Project,
                          files: Map[String, String],
                          externalFiles: Option[Map[String, ExternalFile]],
                          schemaContent: String,
                          isDryRun: Boolean)(implicit inj: Injector)
    extends Injectable {

  val functionEnvironment: FunctionEnvironment = inject[FunctionEnvironment]

  def determineActionsForRemove: RemoveModuleActions = {
    RemoveModuleActions(
      subscriptionFunctionsToRemove = subscriptionFunctionsToRemove,
      schemaExtensionFunctionsToRemove = schemaExtensionFunctionsToRemove,
      operationFunctionsToRemove = operationFunctionsToRemove,
      modelPermissionsToRemove = modelPermissionsToRemove,
      relationPermissionsToRemove = relationPermissionsToRemove,
      rootTokensToRemove = rootTokensToRemove
    )
  }

  def determineActionsForAdd: AddModuleActions = {
    AddModuleActions(
      subscriptionFunctionsToAdd = subscriptionFunctionsToAdd,
      schemaExtensionFunctionsToAdd = schemaExtensionFunctionsToAdd,
      operationFunctionsToAdd = operationFunctionsToAdd,
      modelPermissionsToAdd = modelPermissionsToAdd,
      relationPermissionsToAdd = relationPermissionsToAdd,
      rootTokensToCreate = rootTokensToCreate
    )
  }

  def determineActionsForUpdate: UpdateModuleActions = {
    UpdateModuleActions(
      subscriptionFunctionsToUpdate = subscriptionFunctionsToUpdate,
      schemaExtensionFunctionsToUpdate = schemaExtensionFunctionsToUpdate,
      operationFunctionsToUpdate = operationFunctionsToUpdate
    )
  }

  val auth0Extend: Auth0Extend = inject[Auth0Extend]

  private def getFileContent(filePath: String) = files.getOrElse(filePath, sys.error(s"File with path '$filePath' does not exist"))

  lazy val subscriptionFunctionsToAdd: Vector[AddServerSideSubscriptionFunctionAction] =
    functionDiff.addedSubscriptionFunctions.map {
      case FunctionWithFiles(name, function, fc) =>
        val (code, extendFunction, webhookUrl, headers) = setupFunction(name, function, client)

        val input = AddServerSideSubscriptionFunctionInput(
          clientMutationId = None,
          projectId = project.id,
          name = name,
          isActive = true,
          query = getFileContent(function.query.getOrElse(sys.error("query file path expected"))),
          functionType = function.handlerType,
          url = webhookUrl,
          headers = headers,
          inlineCode = code,
          auth0Id = extendFunction.map(_.auth0Id),
          codeFilePath = fc.codeContainer.map(_.path),
          queryFilePath = fc.queryContainer.map(_.path)
        )
        AddServerSideSubscriptionFunctionAction(input)
    }

  lazy val subscriptionFunctionsToUpdate: Vector[UpdateServerSideSubscriptionFunctionAction] =
    functionDiff.updatedSubscriptionFunctions.map {
      case FunctionWithFiles(name, function, fc) =>
        val (code, extendFunction, webhookUrl, headers) = setupFunction(name, function, client)

        val functionId = project.getFunctionByName_!(name).id

        val input = UpdateServerSideSubscriptionFunctionInput(
          clientMutationId = None,
          functionId = functionId,
          name = Some(name),
          isActive = Some(true),
          query = Some(getFileContent(function.query.getOrElse(sys.error("query file path expected")))),
          functionType = Some(function.handlerType),
          webhookUrl = webhookUrl,
          headers = headers,
          inlineCode = code,
          auth0Id = extendFunction.map(_.auth0Id)
        )
        UpdateServerSideSubscriptionFunctionAction(input)
    }

  lazy val schemaExtensionFunctionsToAdd: Vector[AddSchemaExtensionFunctionAction] =
    functionDiff.addedSchemaExtensionFunctions.map {
      case FunctionWithFiles(name, function, fc) =>
        val (code, extendFunction, webhookUrl, headers) = setupFunction(name, function, client)

        val input = AddSchemaExtensionFunctionInput(
          clientMutationId = None,
          projectId = project.id,
          name = name,
          isActive = true,
          schema = getFileContent(function.schema.getOrElse(sys.error("schema file path expected"))),
          functionType = function.handlerType,
          url = webhookUrl,
          headers = headers,
          inlineCode = code,
          auth0Id = extendFunction.map(_.auth0Id),
          codeFilePath = fc.codeContainer.map(_.path),
          schemaFilePath = fc.schemaContainer.map(_.path)
        )

        AddSchemaExtensionFunctionAction(input)
    }

  lazy val schemaExtensionFunctionsToUpdate: Vector[UpdateSchemaExtensionFunctionAction] =
    functionDiff.updatedSchemaExtensionFunctions.map {
      case FunctionWithFiles(name, function, fc) =>
        val (code, extendFunction, webhookUrl, headers) = setupFunction(name, function, client)

        val functionId = project.getFunctionByName_!(name).id

        val input = UpdateSchemaExtensionFunctionInput(
          clientMutationId = None,
          functionId = functionId,
          name = Some(name),
          isActive = Some(true),
          schema = Some(getFileContent(function.schema.getOrElse(sys.error("schema file path expected")))),
          functionType = Some(function.handlerType),
          webhookUrl = webhookUrl,
          headers = headers,
          inlineCode = code,
          auth0Id = extendFunction.map(_.auth0Id),
          codeFilePath = fc.codeContainer.map(_.path),
          schemaFilePath = fc.schemaContainer.map(_.path)
        )

        UpdateSchemaExtensionFunctionAction(input)
    }

  lazy val operationFunctionsToAdd: Vector[AddOperationFunctionAction] =
    functionDiff.addedRequestPipelineFunctions.map {
      case FunctionWithFiles(name, function, fc) =>
        val x         = function.operation.getOrElse(sys.error("operation is required for subscription function")).split("\\.").toVector
        val modelName = x(0)
        val operation = x(1)

        val rpOperation = operation match {
          case "create" => RequestPipelineOperation.CREATE
          case "delete" => RequestPipelineOperation.DELETE
          case "update" => RequestPipelineOperation.UPDATE
          case invalid  => throw SystemErrors.InvalidRequestPipelineOperation(invalid)
        }

        val modelId = project.getModelByName(modelName) match {
          case Some(existingModel) => existingModel.id
          case None                => sys.error(s"Error in ${function.`type`} function '$name': No model with name '$modelName' found. Please supply a valid model.")
        }

        val (code, extendFunction, webhookUrl, headers) = setupFunction(name, function, client)

        val input = AddRequestPipelineMutationFunctionInput(
          clientMutationId = None,
          projectId = project.id,
          name = name,
          isActive = true,
          functionType = function.handlerType,
          binding = function.binding,
          modelId = modelId,
          operation = rpOperation,
          webhookUrl = webhookUrl,
          headers = headers,
          inlineCode = code,
          auth0Id = extendFunction.map(_.auth0Id),
          codeFilePath = fc.codeContainer.map(_.path)
        )

        AddOperationFunctionAction(input)
    }

  lazy val operationFunctionsToUpdate: Vector[UpdateOperationFunctionAction] =
    functionDiff.updatedRequestPipelineFunctions.map {
      case FunctionWithFiles(name, function, fc) =>
        val x         = function.operation.getOrElse(sys.error("operation is required for subscription function")).split("\\.").toVector
        val modelName = x(0)
        val operation = x(1)
        val rpOperation = operation match {
          case "create" => RequestPipelineOperation.CREATE
          case "delete" => RequestPipelineOperation.DELETE
          case "update" => RequestPipelineOperation.UPDATE
          case invalid  => throw SystemErrors.InvalidRequestPipelineOperation(invalid)
        }

        val modelId = project.getModelByName(modelName) match {
          case Some(existingModel) => existingModel.id
          case None                => sys.error(s"Error in ${function.`type`} function '$name': No model with name '$modelName' found. Please supply a valid model.")
        }

        val functionId = project.getFunctionByName_!(name).id

        val (code, extendFunction, webhookUrl, headers) = setupFunction(name, function, client)

        val input = UpdateRequestPipelineMutationFunctionInput(
          clientMutationId = None,
          functionId = functionId,
          name = Some(name),
          isActive = Some(true),
          functionType = Some(function.handlerType),
          binding = Some(function.binding),
          modelId = Some(modelId),
          operation = Some(rpOperation),
          webhookUrl = webhookUrl,
          headers = headers,
          inlineCode = code,
          auth0Id = extendFunction.map(_.auth0Id)
        )

        UpdateOperationFunctionAction(input)
    }

  /**
    *
    * Determine if the function is webhook, auth0Extend or a normal code handler.
    * Return corresponding function details
    */
  def setupFunction(name: String, function: Ast.Function, client: Client): (Option[String], Option[Auth0FunctionData], Option[String], Option[String]) = {
    val code: Option[String]               = function.handler.code.flatMap(x => files.get(x.src))
    val externalFile: Option[ExternalFile] = function.handler.code.flatMap(x => externalFiles.flatMap(_.get(x.src)))

    (code, externalFile) match {

      case (Some(codeContent), _) => // Auth0 Extend
        val extendFunction: Auth0FunctionData = createAuth0Function(client = client, code = codeContent, functionName = name)

        (Some(codeContent), Some(extendFunction), Some(extendFunction.url), None)
      case (None, Some(externalFileContent)) => // Normal Code Handler
        deployFunctionToRuntime(project, externalFileContent, name) match {
          case DeployFailure(e) => throw e
          case _                =>
        }

        (None, None, None, None)
      case _ => // Webhook
        val webhookUrl: String =
          function.handler.webhook.map(_.url).getOrElse(sys.error("webhook url or inline code required"))

        val headerMap               = function.handler.webhook.map(_.headers)
        val jsonHeader              = headerMap.map(value => JsObject(value.map { case (key, other) => (key, JsString(other)) }))
        val headers: Option[String] = jsonHeader.map(_.toString)

        (code, None, Some(webhookUrl), headers)
    }

  }

  lazy val subscriptionFunctionsToRemove: Vector[RemoveSubscriptionFunctionAction] =
    functionDiff.removedSubscriptionFunctions.map {
      case FunctionWithFiles(name, function, _) =>
        val input = DeleteFunctionInput(
          clientMutationId = None,
          functionId = project.getFunctionByName_!(name).id
        )

        RemoveSubscriptionFunctionAction(input, name)
    }

  lazy val schemaExtensionFunctionsToRemove: Vector[RemoveSchemaExtensionFunctionAction] =
    functionDiff.removedSchemaExtensionFunctions.map {
      case FunctionWithFiles(name, function, _) =>
        val input = DeleteFunctionInput(
          clientMutationId = None,
          functionId = project.getFunctionByName_!(name).id
        )

        RemoveSchemaExtensionFunctionAction(input, name)
    }

  lazy val operationFunctionsToRemove: Vector[RemoveOperationFunctionAction] =
    functionDiff.removedRequestPipelineFunctions.map {
      case FunctionWithFiles(name, function, _) =>
        val input = DeleteFunctionInput(
          clientMutationId = None,
          functionId = project.getFunctionByName_!(name).id
        )

        RemoveOperationFunctionAction(input, name)
    }

  lazy val modelPermissionsToAdd: Vector[AddModelPermissionAction] = permissionDiff.addedModelPermissions.map(permission => {

    val astPermission = permission.permission.permission
    val x             = astPermission.operation.split("\\.").toVector
    val modelName     = x(0)
    val operation     = x(1)
    val modelOperation = operation match {
      case "create" => ModelOperation.Create
      case "read"   => ModelOperation.Read
      case "update" => ModelOperation.Update
      case "delete" => ModelOperation.Delete
      case _        => sys.error(s"Wrong operation defined for ModelPermission. You supplied: '${astPermission.operation}'")
    }

    val userType      = if (astPermission.authenticated) { UserType.Authenticated } else { UserType.Everyone }
    val fileContainer = permission.permission.queryFile
    val rule          = if (fileContainer.isDefined) { CustomRule.Graph } else { CustomRule.None }
    val fieldIds = astPermission.fields match {
      case Some(fieldNames) => fieldNames.map(fieldName => permission.model.getFieldByName_!(fieldName).id)
      case None             => Vector.empty
    }

    val input = AddModelPermissionInput(
      clientMutationId = None,
      modelId = permission.model.id,
      operation = modelOperation,
      userType = userType,
      rule = rule,
      ruleName = getRuleNameFromPath(astPermission.queryPath),
      ruleGraphQuery = fileContainer.map(_.content),
      ruleGraphQueryFilePath = astPermission.queryPath,
      ruleWebhookUrl = None,
      fieldIds = fieldIds.toList,
      applyToWholeModel = astPermission.fields.isEmpty,
      description = astPermission.description,
      isActive = true
    )
    val modelPermissionName = s"$modelName.$modelOperation"
    AddModelPermissionAction(input, modelPermissionName)
  })

  lazy val relationPermissionsToAdd: Vector[AddRelationPermissionAction] = permissionDiff.addedRelationPermissions.map(permission => {

    val astPermission = permission.permission.permission
    val x             = astPermission.operation.split("\\.").toVector
    val relationName  = x(0)
    val operation     = x(1)
    val (connect, disconnect) = operation match {
      case "connect"    => (true, false)
      case "disconnect" => (false, true)
      case "*"          => (true, true)
      case _            => sys.error(s"Wrong operation defined for RelationPermission. You supplied: '${astPermission.operation}'")
    }

    val userType      = if (astPermission.authenticated) { UserType.Authenticated } else { UserType.Everyone }
    val fileContainer = permission.permission.queryFile
    val rule          = if (fileContainer.isDefined) { CustomRule.Graph } else { CustomRule.None }

    val input = AddRelationPermissionInput(
      clientMutationId = None,
      relationId = permission.relation.id,
      connect = connect,
      disconnect = disconnect,
      userType = userType,
      rule = rule,
      ruleName = getRuleNameFromPath(astPermission.queryPath),
      ruleGraphQuery = fileContainer.map(_.content),
      ruleGraphQueryFilePath = astPermission.queryPath,
      ruleWebhookUrl = None,
      description = astPermission.description,
      isActive = true
    )

    val relationPermissionName = s"$relationName.$operation"
    AddRelationPermissionAction(input, relationPermissionName, operation)
  })

  lazy val modelPermissionsToRemove: Vector[RemoveModelPermissionAction] = permissionDiff.removedPermissionIds
    .flatMap(project.getModelPermissionById)
    .map(permission => {
      val input               = DeleteModelPermissionInput(clientMutationId = None, modelPermissionId = permission.id)
      val operation           = permission.operation
      val modelPermissionName = project.getModelByModelPermissionId_!(permission.id).name + "." + operation

      RemoveModelPermissionAction(input, modelPermissionName, operation.toString)
    })

  lazy val relationPermissionsToRemove: Vector[RemoveRelationPermissionAction] = permissionDiff.removedPermissionIds
    .flatMap(project.getRelationPermissionById)
    .map(permission => {
      val input                  = DeleteRelationPermissionInput(clientMutationId = None, relationPermissionId = permission.id)
      val operation              = if (permission.connect && permission.disconnect) "*" else if (permission.connect) "connect" else "disconnect"
      val relationPermissionName = project.getRelationByRelationPermissionId_!(permission.id).name + "." + operation
      RemoveRelationPermissionAction(input, relationPermissionName, operation)
    })

  lazy val rootTokensToRemove: Vector[RemoveRootTokenAction] = rootTokenDiff.removedRootTokensIds
    .flatMap(project.getRootTokenById)
    .map(rootToken => {
      val input         = DeleteRootTokenInput(clientMutationId = None, rootTokenId = rootToken.id)
      val rootTokenName = rootToken.name
      RemoveRootTokenAction(input, rootTokenName)
    })

  lazy val rootTokensToCreate: Vector[CreateRootTokenAction] = rootTokenDiff.addedRootTokens
    .map(rootTokenName => {
      val input = CreateRootTokenInput(clientMutationId = None, projectId = project.id, name = rootTokenName, description = None)
      CreateRootTokenAction(input, rootTokenName)
    })

  // todo: move this around so we don't have to use Await.result
  def createAuth0Function(client: Client, code: String, functionName: String): Auth0FunctionData = {
    if (isDryRun) {
      Auth0FunctionData("dryRun.url", "dryRun-id")
    }
    try {
      val future = auth0Extend.createAuth0Function(client, code)
      Await.result(future, Duration.Inf)
    } catch {
      case _: Throwable => throw ProjectPushError(description = s"Could not create serverless function for '$functionName'. Ensure that the code is valid")
    }

  }

  def deployFunctionToRuntime(project: Project, externalFile: ExternalFile, functionName: String): DeployResponse = {
    if (isDryRun) {
      DeploySuccess()
    } else {
      Await.result(functionEnvironment.deploy(project, externalFile, functionName), Duration.Inf)
    }
  }
}
