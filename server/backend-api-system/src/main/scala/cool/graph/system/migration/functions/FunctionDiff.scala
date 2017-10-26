package cool.graph.system.migration.functions

import cool.graph.shared.models
import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.{Auth0Function, Project, SchemaExtensionFunction, ServerSideSubscriptionFunction}
import cool.graph.system.migration.ProjectConfig
import cool.graph.system.migration.ProjectConfig.Ast.Function
import cool.graph.system.migration.ProjectConfig.{Ast, FileContainerBundle, FunctionWithFiles}
import cool.graph.system.migration.project.FileContainer

case class FunctionDiff(oldProject: Project, oldModule: Ast.Module, functions: Map[String, Function], files: Map[String, String]) {
  import cool.graph.system.migration.Diff

  def functionNames: Vector[String]      = functions.keys.toVector
  def function_!(name: String): Function = functions(name)
  def namedFunction_!(name: String, files: Map[String, String]): FunctionWithFiles = {
    val function: Function = functions(name)

    val fileContainerBundle = {
      def createFileContainer(codePath: Option[String]) = codePath.flatMap(path => files.get(path).map(content => FileContainer(path, content)))

      val codePath   = function.handler.code.map(_.src)
      val schemaPath = function.schema
      val queryPath  = function.query

      FileContainerBundle(codeContainer = createFileContainer(codePath),
                          schemaContainer = createFileContainer(schemaPath),
                          queryContainer = createFileContainer(queryPath))
    }

    FunctionWithFiles(name = name, function = function, fileContainers = fileContainerBundle)
  }

  private val addedFunctions: Vector[String] = functionNames diff oldModule.functionNames

  private val removedFunctions: Vector[String] = oldModule.functionNames diff functionNames
  private val functionsWithSameName: Vector[UpdatedFunction] = {
    val potentiallyUpdatedFunctions = functionNames diff addedFunctions
    val functionsWithSameName = {
      potentiallyUpdatedFunctions
        .map { functionName =>
          val oldFunction: Ast.Function           = oldModule.function_!(functionName)
          val oldProjectFunction: models.Function = oldProject.getFunctionByName(functionName).getOrElse(sys.error("that is supposed to be there..."))
          val newFunction: Ast.Function           = function_!(functionName)

          val oldSchema = oldProjectFunction match {
            case x: SchemaExtensionFunction => Some(x.schema)
            case _                          => None
          }
          val oldSchemaFilePath = oldProjectFunction match {
            case x: SchemaExtensionFunction => x.schemaFilePath
            case _                          => None
          }

          val oldCode = oldProjectFunction.delivery match {
            case x: Auth0Function => Some(x.code)
            case _                => None
          }
          val oldCodeFilePath = oldProjectFunction.delivery match {
            case x: Auth0Function => x.codeFilePath
            case _                => None
          }

          val oldQuery = oldProjectFunction match {
            case x: ServerSideSubscriptionFunction => Some(x.query)
            case _                                 => None
          }
          val oldQueryFilePath = oldProjectFunction match {
            case x: ServerSideSubscriptionFunction => x.queryFilePath
            case _                                 => None
          }

          val x = UpdatedFunction(
            name = functionName,
            description = Diff.diffOpt(oldFunction.description, newFunction.description),
            handlerWebhookUrl = Diff.diffOpt(oldFunction.handler.webhook.map(_.url), newFunction.handler.webhook.map(_.url)),
            handlerWebhookHeaders = Diff.diffOpt(oldFunction.handler.webhook.map(_.headers), newFunction.handler.webhook.map(_.headers)),
            handlerCodeSrc = Diff.diffOpt(oldFunction.handler.code.map(_.src), newFunction.handler.code.map(_.src)),
            binding = Diff.diff(oldFunction.binding, newFunction.binding),
            schema = Diff.diffOpt(oldSchema, newFunction.schema.map(path => files.getOrElse(path, sys.error("The schema file path was not supplied")))),
            schemaFilePath = Diff.diffOpt(oldSchemaFilePath, newFunction.schema),
            code = Diff.diffOpt(oldCode, newFunction.handler.code.flatMap(x => files.get(x.src))),
            codeFilePath = Diff.diffOpt(oldCodeFilePath, newFunction.handler.code.map(_.src)), // this triggers the diff for functions with external files , we need this to redeploy them on every push
            query = Diff.diffOpt(oldQuery, newFunction.query.map(path => files.getOrElse(path, sys.error("The query file path was not supplied")))),
            queryFilePath = Diff.diffOpt(oldQueryFilePath, newFunction.query),
            operation = Diff.diffOpt(oldFunction.operation, newFunction.operation),
            `type` = Diff.diff(oldFunction.`type`, newFunction.`type`)
          )
          x
        }
        .filter(_.hasChanges)
    }
    functionsWithSameName
  }

  // updated functions that have a binding change are not really updated.
  // in this case it is the deletion of a function with the old binding and afterwards the creation of a function with the same name under the new binding

  // for a real update we should introduce a way to keep the logs once we have a migrationConcept

  val differentFunctionsUnderSameName = functionsWithSameName.filter(updatedFunction => updatedFunction.binding.nonEmpty)
  val updatedFunctions                = functionsWithSameName.filter(updatedFunction => updatedFunction.binding.isEmpty)

  val namedUpdatedFunctions = updatedFunctions.map(x => namedFunction_!(x.name, files))
  val namedAddedFunctions   = (addedFunctions ++ differentFunctionsUnderSameName.map(_.name)).map(namedFunction_!(_, files))
  val namedRemovedFunctions = (removedFunctions ++ differentFunctionsUnderSameName.map(_.name)).map(oldModule.namedFunction_!(_, files))

  def isRequestPipelineFunction(x: ProjectConfig.FunctionWithFiles) = x.function.`type` == "operationBefore" || x.function.`type` == "operationAfter"

  val updatedSubscriptionFunctions    = namedUpdatedFunctions.filter(_.function.`type` == "subscription")
  val updatedRequestPipelineFunctions = namedUpdatedFunctions.filter(isRequestPipelineFunction)
  val updatedSchemaExtensionFunctions = namedUpdatedFunctions.filter(_.function.`type` == "resolver")

  val addedSubscriptionFunctions    = namedAddedFunctions.filter(_.function.`type` == "subscription")
  val addedRequestPipelineFunctions = namedAddedFunctions.filter(isRequestPipelineFunction)
  val addedSchemaExtensionFunctions = namedAddedFunctions.filter(_.function.`type` == "resolver")

  val removedSubscriptionFunctions    = namedRemovedFunctions.filter(_.function.`type` == "subscription")
  val removedRequestPipelineFunctions = namedRemovedFunctions.filter(isRequestPipelineFunction)
  val removedSchemaExtensionFunctions = namedRemovedFunctions.filter(_.function.`type` == "resolver")

}

case class UpdatedFunction(
    name: String,
    description: Option[String],
    handlerWebhookUrl: Option[String],
    handlerWebhookHeaders: Option[Map[String, String]],
    handlerCodeSrc: Option[String],
    binding: Option[FunctionBinding],
    schema: Option[String],
    schemaFilePath: Option[String],
    code: Option[String],
    codeFilePath: Option[String],
    query: Option[String],
    queryFilePath: Option[String],
    operation: Option[String],
    `type`: Option[String]
) {
  def hasChanges: Boolean = {
    description.nonEmpty || handlerWebhookUrl.nonEmpty || handlerWebhookHeaders.nonEmpty || handlerCodeSrc.nonEmpty || schema.nonEmpty || schemaFilePath.nonEmpty || code.nonEmpty || codeFilePath.nonEmpty || query.nonEmpty || queryFilePath.nonEmpty || operation.nonEmpty || `type`.nonEmpty
  }

}
