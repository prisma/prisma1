package cool.graph.system.migration

import cool.graph.shared.errors.{SystemErrors, UserInputErrors}
import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models._
import cool.graph.system.migration.dataSchema.SchemaExport
import cool.graph.system.migration.permissions.QueryPermissionHelper
import cool.graph.system.migration.project.FileContainer
import net.jcazevedo.moultingyaml._
import org.yaml.snakeyaml.scanner.ScannerException
import scaldi.Injector

import scala.collection.immutable

//Todo add error handling to parse and print

object ProjectConfig {
  def parse(config: String): Ast.Module = {
    implicit val protocol = Ast.ConfigProtocol.ModuleFormat
    try {
      config.parseYaml.convertTo[Ast.Module]
    } catch {
      case e: DeserializationException => throw UserInputErrors.InvalidSchema(s"Parsing of the Yaml failed: ${e.msg}")
      case e: ScannerException         => throw UserInputErrors.InvalidSchema(s"Parsing of the Yaml failed: ${e.getMessage}")
    }
  }

  def print(module: Ast.Module): String = {
    implicit val protocol = Ast.ConfigProtocol.ModuleFormat

    val printedYaml = module.toYaml.prettyPrint

    // Our Yaml library does not concern itself with comments and spacing
    printedYaml
      .replaceAllLiterally("functions: {}", emptyFunctionsRendering)
      .replaceAllLiterally("permissions: []", "# permissions: []")
      .replaceAllLiterally("rootTokens: []", "# rootTokens: []")
  }

  def print(project: Project): String = {
    moduleFromProject(project).module.print
  }

  def moduleFromProject(project: Project): ModuleAndFiles = {
    val types                                            = typesFromProject(project)
    val namedFunctions: immutable.Seq[FunctionWithFiles] = functionsFromProject(project)
    val permissionBundlesWithId                          = permissionBundlesFromProject(project)
    val permissions                                      = permissionBundlesWithId.flatMap(_.permissions).map(_.astPermission).toVector

    // only create the output path here

    val module = Ast.Module(
      types = Some(types.path),
      functions = namedFunctions.map(x => (x.name, x.function)).toMap,
      permissions = permissions,
      rootTokens = project.rootTokens.map(_.name).toVector
    )

    val files = Vector(types) ++ namedFunctions.flatMap(f =>
      List(f.fileContainers.codeContainer, f.fileContainers.queryContainer, f.fileContainers.schemaContainer).flatten) ++ permissionBundlesWithId.flatMap(
      _.fileContainer)

    ModuleAndFiles(module, files)
  }

  private def typesFromProject(project: Project): FileContainer = {
    FileContainer(path = "./types.graphql", SchemaExport.renderSchema(project))
  }

  def permissionBundleFromModel(model: Model, project: Project): PermissionBundle = {

    val permissions = model.permissions
      .filter(_.isActive)
      .map { permission =>
        val otherPermissionsWithSameOperationIds = model.permissions.filter(_.operation == permission.operation).map(_.id)
        val alternativeRuleName: String =
          QueryPermissionHelper.generateAlternativeRuleName(otherPermissionsWithSameOperationIds, permission.id, permission.operationString)

        val (queryPath, query) = QueryPermissionHelper.queryAndQueryPathFromModelPermission(model, permission, alternativeRuleName, project)

        val astPermission = Ast.Permission(
          description = permission.description,
          operation = s"${model.name}.${permission.operationString}",
          authenticated = permission.userType == UserType.Authenticated,
          queryPath = queryPath,
          fields = if (permission.applyToWholeModel) {
            None
          } else {
            Some(permission.fieldIds.toVector.map(id => model.getFieldById_!(id).name))
          }
        )
        AstPermissionWithAllInfos(astPermission, query, queryPath, permission.id)
      }
      .toVector

    val containerPath                        = s"./src/permissions/${model.name}"
    val fileContainer: Option[FileContainer] = QueryPermissionHelper.bundleQueriesInOneFile(queries = permissions.flatMap(_.query), containerPath)

    PermissionBundle(permissions, fileContainer)
  }

  def permissionBundleFromRelation(relation: Relation, project: Project): PermissionBundle = {
    val permissions = relation.permissions
      .filter(_.isActive)
      .map { permission =>
        val otherPermissionsWithSameOperationIds = relation.permissions.filter(_.operation == permission.operation).map(_.id)
        val alternativeRuleName: String =
          QueryPermissionHelper.generateAlternativeRuleName(otherPermissionsWithSameOperationIds, permission.id, permission.operationString)

        val (queryPath, query) = QueryPermissionHelper.queryAndQueryPathFromRelationPermission(relation, permission, alternativeRuleName, project)

        val astPermission = Ast.Permission(
          description = permission.description,
          operation = s"${relation.name}.${permission.operation}",
          authenticated = permission.userType == UserType.Authenticated,
          queryPath = queryPath
        )

        AstPermissionWithAllInfos(astPermission, query, queryPath, permission.id)
      }
      .toVector

    val containerPath                        = s"./src/permissions/${relation.name}"
    val fileContainer: Option[FileContainer] = QueryPermissionHelper.bundleQueriesInOneFile(queries = permissions.flatMap(_.query), containerPath)

    PermissionBundle(permissions, fileContainer)
  }

  // this should only be used in project config not in the permission diff
  def permissionBundlesFromProject(project: Project): List[PermissionBundle] = {
    val modelsWithPermissions       = project.models.filter(_.permissions.nonEmpty)
    val modelsWithActivePermissions = modelsWithPermissions.filter(model => model.permissions.exists(_.isActive == true))

    val modelPermissionBundles = modelsWithActivePermissions.map(model => permissionBundleFromModel(model, project))

    val relationsWithPermissions       = project.relations.filter(_.permissions.nonEmpty)
    val relationsWithActivePermissions = relationsWithPermissions.filter(relation => relation.permissions.exists(_.isActive == true))

    val relationPermissionBundles = relationsWithActivePermissions.map(relation => permissionBundleFromRelation(relation, project))

    modelPermissionBundles ++ relationPermissionBundles
  }

  private def functionsFromProject(project: Project): Vector[FunctionWithFiles] = {

    def getHandler(function: Function): Ast.FunctionHandler = {
      function.delivery match {
        case x: WebhookFunction =>
          Ast.FunctionHandler(webhook = Some(Ast.FunctionHandlerWebhook(url = x.url, headers = x.headers.toMap)))

        case x: Auth0Function =>
          val path = x.codeFilePath match {
            case Some(string) => string
            case None         => defaultPathForFunctionCode(function.name)
          }
          Ast.FunctionHandler(code = Some(Ast.FunctionHandlerCode(src = path)))
        // todo: how do we check changes to the actual file
        case x: ManagedFunction =>
          val path = x.codeFilePath match {
            case Some(string) => string
            case None         => defaultPathForFunctionCode(function.name)
          }
          Ast.FunctionHandler(code = Some(Ast.FunctionHandlerCode(src = path)))
      }
    }

    def getHandlerFileContainer(function: Function) = {
      function.delivery match {
        case _: WebhookFunction =>
          None

        case x: Auth0Function =>
          Some(x.codeFilePath match {
            case Some(path) => FileContainer(path = path, content = x.code)
            case None       => FileContainer(path = defaultPathForFunctionCode(function.name), content = x.code)
          })

        case x: ManagedFunction => None
      }
    }

    project.functions.filter(_.isActive).toVector collect {
      case x: ServerSideSubscriptionFunction =>
        val queryFileContainer = Some(x.queryFilePath match {
          case Some(path) => FileContainer(path = path, content = x.query)
          case None       => FileContainer(path = defaultPathForFunctionQuery(x.name), content = x.query)
        })

        FunctionWithFiles(
          name = x.name,
          function = Ast.Function(
            description = None,
            handler = getHandler(x),
            `type` = "subscription",
            query = queryFileContainer.map(_.path) // todo: how do we check changes to the actual file
          ),
          fileContainers = FileContainerBundle(queryContainer = queryFileContainer, codeContainer = getHandlerFileContainer(x))
        )
      case x: CustomMutationFunction =>
        val schemaFileContainer = Some(x.schemaFilePath match {
          case Some(path) => FileContainer(path = path, content = x.schema)
          case None       => FileContainer(path = defaultPathForFunctionSchema(x.name), content = x.schema)
        })

        FunctionWithFiles(
          name = x.name,
          function = Ast.Function(
            description = None,
            handler = getHandler(x),
            `type` = "resolver",
            schema = schemaFileContainer.map(_.path)
          ),
          fileContainers = FileContainerBundle(schemaContainer = schemaFileContainer, codeContainer = getHandlerFileContainer(x))
        )
      case x: CustomQueryFunction =>
        val schemaFileContainer = Some(x.schemaFilePath match {
          case Some(path) => FileContainer(path = path, content = x.schema)
          case None       => FileContainer(path = defaultPathForFunctionSchema(x.name), content = x.schema)
        })

        FunctionWithFiles(
          name = x.name,
          function = Ast.Function(
            description = None,
            handler = getHandler(x),
            `type` = "resolver",
            schema = schemaFileContainer.map(_.path)
          ),
          fileContainers = FileContainerBundle(schemaContainer = schemaFileContainer, codeContainer = getHandlerFileContainer(x))
        )
      case x: RequestPipelineFunction if x.binding == FunctionBinding.TRANSFORM_ARGUMENT =>
        FunctionWithFiles(
          name = x.name,
          function = Ast.Function(
            description = None,
            handler = getHandler(x),
            `type` = "operationBefore",
            operation = Some(project.getModelById_!(x.modelId).name + "." + x.operation.toString.toLowerCase)
          ),
          fileContainers = FileContainerBundle(codeContainer = getHandlerFileContainer(x))
        )
      case x: RequestPipelineFunction if x.binding == FunctionBinding.TRANSFORM_PAYLOAD =>
        FunctionWithFiles(
          name = x.name,
          function = Ast.Function(
            description = None,
            handler = getHandler(x),
            `type` = "operationAfter",
            operation = Some(project.getModelById_!(x.modelId).name + "." + x.operation.toString.toLowerCase)
          ),
          fileContainers = FileContainerBundle(codeContainer = getHandlerFileContainer(x))
        )
    }
  }

  object Ast {
    case class Module(types: Option[String] = None,
                      functions: Map[String, Function] = Map.empty,
                      modules: Option[Map[String, String]] = None,
                      permissions: Vector[Permission] = Vector.empty,
                      rootTokens: Vector[String] = Vector.empty) {
      def functionNames: Vector[String] = functions.keys.toVector
      def function_!(name: String)      = functions(name)
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

      def print: String = {
        implicit val protocol = Ast.ConfigProtocol.ModuleFormat
        this.toYaml.prettyPrint
      }
    }

    case class Function(description: Option[String] = None,
                        handler: FunctionHandler,
                        `type`: String,
                        schema: Option[String] = None,
                        query: Option[String] = None,
                        operation: Option[String] = None) {
      def binding: FunctionBinding = `type` match {
        case "httpRequest"  => FunctionBinding.TRANSFORM_REQUEST
        case "httpResponse" => FunctionBinding.TRANSFORM_RESPONSE
        case "resolver"     => FunctionBinding.CUSTOM_MUTATION // todo: determine if mutation or query
        //case "resolver" => FunctionBinding.CUSTOM_QUERY // todo: determine if mutation or query
        case "subscription"    => FunctionBinding.SERVERSIDE_SUBSCRIPTION
        case "operationBefore" => FunctionBinding.TRANSFORM_ARGUMENT
        case "operationAfter"  => FunctionBinding.TRANSFORM_PAYLOAD
        case invalid           => throw SystemErrors.InvalidFunctionType(invalid)
      }

      def handlerType: cool.graph.shared.models.FunctionType.FunctionType = handler match {
        case x if x.webhook.isDefined => cool.graph.shared.models.FunctionType.WEBHOOK
        case x if x.code.isDefined    => cool.graph.shared.models.FunctionType.CODE
      }
    }

    case class FunctionHandler(webhook: Option[FunctionHandlerWebhook] = None, code: Option[FunctionHandlerCode] = None)
    case class FunctionHandlerWebhook(url: String, headers: Map[String, String] = Map.empty)
    case class FunctionHandlerCode(src: String)
    case class FunctionType(subscription: Option[String] = None,
                            httpRequest: Option[HttpRequest] = None,
                            httpResponse: Option[HttpResponse] = None,
                            schemaExtension: Option[String] = None,
                            operationBefore: Option[String] = None,
                            operationAfter: Option[String] = None)
    case class FunctionEventSchemaExtension(schema: FunctionEventSchemaExtensionSchema)
    case class FunctionEventSchemaExtensionSchema(src: String)
    case class HttpRequest(order: Int = 0)
    case class HttpResponse(order: Int = 0)
    case class Permission(description: Option[String] = None,
                          operation: String,
                          authenticated: Boolean = false,
                          queryPath: Option[String] = None,
                          fields: Option[Vector[String]] = None)
    case class PermissionQuery(src: String)

    object ConfigProtocol extends DefaultYamlProtocol {

      implicit val PermissionQueryFormat = yamlFormat1(PermissionQuery)

      implicit object PermissionFormat extends YamlFormat[Permission] {
        def write(c: Permission) = {
          var fields: Seq[(YamlValue, YamlValue)] = Vector(YamlString("operation") -> YamlString(c.operation))

          if (c.description.nonEmpty) {
            fields :+= YamlString("description") -> YamlString(c.description.get)
          }
          if (c.authenticated) {
            fields :+= YamlString("authenticated") -> YamlBoolean(true)
          }
          if (c.queryPath.nonEmpty) {
            fields :+= YamlString("query") -> YamlString(c.queryPath.get)
          }
          if (c.fields.nonEmpty) {
            fields :+= YamlString("fields") -> YamlArray(c.fields.get.map(YamlString))
          }

          YamlObject(
            fields: _*
          )
        }
        def read(value: YamlValue) = {
          val fields = value.asYamlObject.fields

          Permission(
            description = fields.get(YamlString("description")).map(_.convertTo[String]),
            operation = fields(YamlString("operation")).convertTo[String],
            authenticated = fields.get(YamlString("authenticated")).map(_.convertTo[Boolean]).getOrElse(false),
            queryPath = fields.get(YamlString("query")).map(_.convertTo[String]),
            fields = fields.get(YamlString("fields")).map(_.convertTo[Vector[String]])
          )
        }
      }

      implicit val HttpRequestFormat                        = yamlFormat1(HttpRequest)
      implicit val HttpResponseFormat                       = yamlFormat1(HttpResponse)
      implicit val FunctionEventSchemaExtensionSchemaFormat = yamlFormat1(FunctionEventSchemaExtensionSchema)
      implicit val FunctionEventSchemaExtensionFormat       = yamlFormat1(FunctionEventSchemaExtension)
      implicit val FunctionEventFormat                      = yamlFormat6(FunctionType)
      implicit val FunctionHandlerCodeFormat                = yamlFormat1(FunctionHandlerCode)

      implicit object FunctionHandlerWebhookFormat extends YamlFormat[FunctionHandlerWebhook] {
        def write(c: FunctionHandlerWebhook) = {
          var fields: Seq[(YamlValue, YamlValue)] = Vector(YamlString("url") -> c.url.toYaml)

          if (c.headers.nonEmpty) {
            fields :+= YamlString("headers") -> c.headers.toYaml
          }

          YamlObject(
            fields: _*
          )
        }
        def read(value: YamlValue) = {
          val fields = value.asYamlObject.fields

          if (fields.get(YamlString("headers")).nonEmpty) {
            FunctionHandlerWebhook(url = fields(YamlString("url")).convertTo[String], headers = fields(YamlString("headers")).convertTo[Map[String, String]])
          } else {
            FunctionHandlerWebhook(url = fields(YamlString("url")).convertTo[String])
          }
        }
      }

      implicit val FunctionHandlerFormat = yamlFormat2(FunctionHandler)

      implicit object FunctionFormat extends YamlFormat[Function] {
        def write(c: Function) = {
          var fields: Seq[(YamlValue, YamlValue)] = Vector(YamlString("handler") -> c.handler.toYaml, YamlString("type") -> c.`type`.toYaml)

          if (c.description.nonEmpty) {
            fields :+= YamlString("description") -> YamlString(c.description.get)
          }
          if (c.schema.nonEmpty) {
            fields :+= YamlString("schema") -> YamlString(c.schema.get)
          }
          if (c.query.nonEmpty) {
            fields :+= YamlString("query") -> YamlString(c.query.get)
          }
          if (c.operation.nonEmpty) {
            fields :+= YamlString("operation") -> YamlString(c.operation.get)
          }

          YamlObject(
            fields: _*
          )
        }

        def read(value: YamlValue) = {
          val fields = value.asYamlObject.fields

          val handler = if (fields(YamlString("handler")).asYamlObject.fields.get(YamlString("code")).exists(_.isInstanceOf[YamlString])) {
            FunctionHandler(code = Some(FunctionHandlerCode(src = fields(YamlString("handler")).asYamlObject.fields(YamlString("code")).convertTo[String])))
          } else {
            fields(YamlString("handler")).convertTo[FunctionHandler]
          }

          Function(
            description = fields.get(YamlString("description")).map(_.convertTo[String]),
            handler = handler,
            `type` = fields(YamlString("type")).convertTo[String],
            schema = fields.get(YamlString("schema")).map(_.convertTo[String]),
            query = fields.get(YamlString("query")).map(_.convertTo[String]),
            operation = fields.get(YamlString("operation")).map(_.convertTo[String])
          )
        }
      }

      implicit object ModuleFormat extends YamlFormat[Module] {
        def write(c: Module) = {
          var fields: Seq[(YamlValue, YamlValue)] = Vector.empty

          if (c.types.nonEmpty) {
            fields :+= YamlString("types") -> YamlString(c.types.get)
          }
          fields :+= YamlString("functions") -> c.functions.toYaml

          if (c.modules.nonEmpty) {
            fields :+= YamlString("modules") -> c.modules.toYaml
          }

          fields :+= YamlString("permissions") -> c.permissions.toYaml

          fields :+= YamlString("rootTokens") -> c.rootTokens.toYaml

          YamlObject(
            fields: _*
          )
        }
        def read(value: YamlValue) = {
          val fields = value.asYamlObject.fields

          Module(
            types = fields.get(YamlString("types")).map(_.convertTo[String]),
            functions = fields.get(YamlString("functions")).map(_.convertTo[Map[String, Function]]).getOrElse(Map.empty),
            modules = fields.get(YamlString("modules")).map(_.convertTo[Map[String, String]]),
            permissions = fields.get(YamlString("permissions")).map(_.convertTo[Vector[Permission]]).getOrElse(Vector.empty),
            rootTokens = fields.get(YamlString("rootTokens")).map(_.convertTo[Vector[String]]).getOrElse(Vector.empty)
          )
        }
      }
    }
  }

  case class FunctionWithFiles(name: String, function: Ast.Function, fileContainers: FileContainerBundle)

  case class PermissionWithQueryFile(permission: Ast.Permission, queryFile: Option[FileContainer])
  case class PermissionWithId(permission: PermissionWithQueryFile, id: String)

  case class AstPermissionWithAllInfos(astPermission: Ast.Permission, query: Option[String], queryPath: Option[String], permissionId: String)

  case class PermissionBundle(permissions: Vector[AstPermissionWithAllInfos], fileContainer: Option[FileContainer])

  case class ModuleAndFiles(module: Ast.Module, files: Vector[FileContainer])

  case class FileContainerBundle(codeContainer: Option[FileContainer] = None,
                                 schemaContainer: Option[FileContainer] = None,
                                 queryContainer: Option[FileContainer] = None)
  def defaultPathForFunctionCode(functionName: String)     = s"./src/$functionName.js"
  def defaultPathForFunctionQuery(functionName: String)    = s"./src/$functionName.graphql"
  def defaultPathForFunctionSchema(functionName: String)   = s"./src/$functionName.graphql"
  def defaultPathForPermissionQuery(generatedName: String) = s"./src/permissions/$generatedName"

  val emptyFunctionsRendering = """# functions:
                                  |#  helloWorld:
                                  |#    handler:
                                  |#      code: |
                                  |#        module.exports = function sum(event) {
                                  |#          const data = event.data
                                  |#          const message = `Hello World (${data.extraMessage})`
                                  |#          return {data: {message: message}}
                                  |#        }
                                  |#    type: resolver
                                  |#    schema: |
                                  |#      type HelloPayload {
                                  |#        message: String!
                                  |#      }
                                  |#
                                  |#      extend type Query {
                                  |#        hello(extraMessage: String): HelloPayload
                                  |#      }""".stripMargin
}
