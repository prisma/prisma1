package cool.graph.system.migration.permissions

import cool.graph.client.UserContext
import cool.graph.shared.errors.UserInputErrors.{ModelOrRelationForPermissionDoesNotExist, QueryPermissionParseError}
import cool.graph.shared.models._
import cool.graph.shared.queryPermissions.PermissionSchemaResolver
import cool.graph.system.migration.ProjectConfig._
import cool.graph.system.migration.permissions.QueryPermissionHelper._
import cool.graph.system.migration.project.FileContainer
import sangria.ast.{Document, OperationDefinition}
import sangria.schema.Schema
import sangria.validation.{QueryValidator, Violation}
import scaldi.Injector

case class PermissionDiff(project: Project, newPermissions: Vector[AstPermissionWithAllInfos], files: Map[String, String], afterSchemaMigration: Boolean)(
    implicit inj: Injector) {
  val containsGlobalStarPermission: Boolean = newPermissions.exists(_.astPermission.operation == "*")

  val oldPermissionsWithId: Vector[AstPermissionWithAllInfos] = astPermissionsWithAllInfosFromProject(project)
  val oldPermissions: Vector[AstPermissionWithAllInfos]       = oldPermissionsWithId.map(astPermissionWithoutId)

  val addedPermissions: Vector[AstPermissionWithAllInfos]   = newPermissions diff oldPermissions
  val removedPermissions: Vector[AstPermissionWithAllInfos] = oldPermissions diff newPermissions

  val removedPermissionIds: Vector[String] = getIdsOfRemovedPermissions

  private def getIdsOfRemovedPermissions = {
    val distinctRemovedPermissions = removedPermissions.distinct
    val distinctWithCount          = distinctRemovedPermissions.map(perm => (perm, removedPermissions.count(_ == perm)))

    distinctWithCount.flatMap {
      case (permission, removedCount) =>
        val oldPerms = oldPermissionsWithId.filter(_.astPermission == permission.astPermission)
        oldPerms.take(removedCount).map(_.permissionId)
    }
  }

  val modelNames    = project.models.map(_.name)
  val relationNames = project.relations.map(_.name)

  val superflousPermissions = addedPermissions
    .filter(p => !modelNames.contains(nameFromOperation(p)) && !relationNames.contains(nameFromOperation(p)))
    .filter(_.astPermission.operation != "*")

  val superflousPermissionOperations = superflousPermissions.map(_.astPermission).map(_.operation)

  if (superflousPermissionOperations.nonEmpty && afterSchemaMigration)
    throw ModelOrRelationForPermissionDoesNotExist(superflousPermissionOperations.mkString(", "))

  val addedModelPermissions: Vector[PermissionWithModel] = addedPermissions.flatMap(permission => {
    val modelName: String = permission.astPermission.operation.split("\\.")(0)

    val fileContainer: Option[FileContainer] = QueryPermissionHelper.fileContainerFromQueryPath(permission.queryPath, files)

    project
      .getModelByName(modelName)
      .map(model => PermissionWithModel(PermissionWithQueryFile(permission.astPermission, fileContainer), model))
  })

  val addedRelationPermissions: Vector[PermissionWithRelation] = addedPermissions.flatMap(permission => {
    val relationName: String = permission.astPermission.operation.split("\\.")(0)

    val fileContainer: Option[FileContainer] = QueryPermissionHelper.fileContainerFromQueryPath(permission.queryPath, files)

    project
      .getRelationByName(relationName)
      .map(relation => PermissionWithRelation(PermissionWithQueryFile(permission.astPermission, fileContainer), relation))
  })

}

case class PermissionWithModel(permission: PermissionWithQueryFile, model: Model)
case class PermissionWithRelation(permission: PermissionWithQueryFile, relation: Relation)

object QueryPermissionHelper {
  import sangria.renderer.QueryRenderer

  def nameFromOperation(permission: AstPermissionWithAllInfos): String = permission.astPermission.operation.split("\\.").head

  def renderQueryForName(queryName: String, path: String, files: Map[String, String]): String = {
    val (queries: String, operationDefinitions: Vector[OperationDefinition]) = operationDefinitionsAndQueriesFromPath(path, files)

    val queryToRender: OperationDefinition =
      operationDefinitions.filter(operationDefinition => operationDefinition.name.contains(queryName)) match {
        case x if x.length == 1 => x.head
        case x if x.length > 1  => throw QueryPermissionParseError(queryName, s"There was more than one query with the name $queryName in the file. $queries")
        case x if x.isEmpty     => throw QueryPermissionParseError(queryName, s"There was no query with the name $queryName in the file. $queries")
      }
    val query = renderQueryWithoutComments(queryToRender)
    query
  }

  def renderQuery(path: String, files: Map[String, String]): String = {
    val (queries: String, operationDefinitions: Vector[OperationDefinition]) = operationDefinitionsAndQueriesFromPath(path, files)

    val queryToRender: OperationDefinition = operationDefinitions match {
      case x if x.length == 1 => x.head
      case x if x.length > 1  => throw QueryPermissionParseError("NoName", s"There was more than one query and you did not provide a query name. $queries")
      case x if x.isEmpty     => throw QueryPermissionParseError("NoName", s"There was no query in the file. $queries")
    }
    val query = renderQueryWithoutComments(queryToRender)
    query
  }

  def renderQueryWithoutComments(input: OperationDefinition): String = QueryRenderer.render(input.copy(comments = Vector.empty))

  def operationDefinitionsAndQueriesFromPath(path: String, files: Map[String, String]): (String, Vector[OperationDefinition]) = {
    val queries = files.get(path) match {
      case Some(string) => string
      case None         => throw QueryPermissionParseError("", s"There was no file for the path: $path provided")
    }

    val doc = sangria.parser.QueryParser.parse(queries).toOption match {
      case Some(document) => document
      case None           => throw QueryPermissionParseError("", s"Query could not be parsed. Please ensure it is valid GraphQL. $queries")
    }

    val operationDefinitions = doc.definitions.collect { case x: OperationDefinition => x }
    (queries, operationDefinitions)
  }

  def splitPathInRuleNameAndPath(path: String): (Option[String], Option[String]) = {
    path match {
      case _ if path.contains(":") =>
        path.split(":") match {
          case Array(one, two, three, _*)     => throw QueryPermissionParseError(two, s"There was more than one colon in your filepath. $path")
          case Array(pathPart, queryNamePart) => (Some(queryNamePart), Some(pathPart))
        }
      case _ => (None, Some(path))
    }
  }

  def getRuleNameFromPath(pathOption: Option[String]): Option[String] = {
    pathOption match {
      case Some(path) => splitPathInRuleNameAndPath(path)._1
      case None       => None
    }
  }

  def astPermissionWithAllInfosFromAstPermission(astPermission: Ast.Permission, files: Map[String, String]): AstPermissionWithAllInfos = {

    astPermission.queryPath match {
      case Some(path) =>
        splitPathInRuleNameAndPath(path) match {
          case (Some(name), Some(pathPart)) =>
            AstPermissionWithAllInfos(astPermission = astPermission,
                                      query = Some(renderQueryForName(name, pathPart, files)),
                                      queryPath = astPermission.queryPath,
                                      permissionId = "")

          case (None, Some(pathPart)) =>
            AstPermissionWithAllInfos(astPermission = astPermission,
                                      query = Some(renderQuery(pathPart, files)),
                                      queryPath = astPermission.queryPath,
                                      permissionId = "")
          case _ =>
            sys.error("This should not happen")

        }
      case None =>
        AstPermissionWithAllInfos(astPermission = astPermission, query = None, queryPath = None, permissionId = "")
    }
  }

  def queryAndQueryPathFromModelPermission(model: Model,
                                           modelPermission: ModelPermission,
                                           alternativeRuleName: String,
                                           project: Project): (Option[String], Option[String]) = {
    modelPermission.rule match {
      case CustomRule.Graph =>
        val args: List[(String, String)] = permissionQueryArgsFromModel(model)
        queryAndQueryPathFromPermission(model.name, modelPermission.ruleName, args, modelPermission.ruleGraphQuery, alternativeRuleName)

      case _ =>
        (None, None)
    }
  }

  def permissionQueryArgsFromModel(model: Model): List[(String, String)] = {
    model.scalarFields.map(field => (s"$$node_${field.name}", TypeIdentifier.toSangriaScalarType(field.typeIdentifier).name))
  }

  def queryAndQueryPathFromRelationPermission(relation: Relation,
                                              relationPermission: RelationPermission,
                                              alternativeRuleName: String,
                                              project: Project): (Option[String], Option[String]) = {
    relationPermission.rule match {
      case CustomRule.Graph =>
        val args: List[(String, String)] = permissionQueryArgsFromRelation(relation, project)
        queryAndQueryPathFromPermission(relation.name, relationPermission.ruleName, args, relationPermission.ruleGraphQuery, alternativeRuleName)

      case _ =>
        (None, None)
    }
  }

  def permissionQueryArgsFromRelation(relation: Relation, project: Project): List[(String, String)] = {
    List(("$user_id", "ID"), (s"$$${relation.aName(project)}_id", "ID"), (s"$$${relation.bName(project)}_id", "ID"))
  }

  def queryAndQueryPathFromPermission(modelOrRelationName: String,
                                      ruleName: Option[String],
                                      args: List[(String, String)],
                                      ruleGraphQuery: Option[String],
                                      alternativeRuleName: String): (Option[String], Option[String]) = {

    val queryName = ruleName match {
      case None    => alternativeRuleName
      case Some(x) => x
    }

    val generatedName = s"$modelOrRelationName.graphql:$queryName"
    val queryPath     = defaultPathForPermissionQuery(generatedName)

    val resultingQuery = ruleGraphQuery.map(query => prependNameAndRenderQuery(query, queryName, args))
    (Some(queryPath), resultingQuery)
  }

  /** This is only a dumb printer
    * it takes a query string and checks whether it is valid GraphQL after prepending
    * it does not do a schema validation of the query
    * it will however format the query using the Sangria Rendering and set the query name
    * the queryName is either the ruleName or the alternative name ([operation][ 1,2...])
    * ---
    * it will discard names on the queries that do not match the ruleName
    * it will take the first query definition it finds and ignore the others
    */
  def prependNameAndRenderQuery(query: String, queryName: String, args: List[(String, String)]): String = {

    def renderQueryWithCorrectNameWithSangria(doc: Document) = {
      val firstDefinition: OperationDefinition                            = doc.definitions.collect { case x: OperationDefinition => x }.head
      val definitionWithQueryName: _root_.sangria.ast.OperationDefinition = firstDefinition.copy(name = Some(queryName))
      renderQueryWithoutComments(definitionWithQueryName)
    }

    def prependQueryWithHeader(query: String) = {
      val usedVars    = args.filter(field => query.contains(field._1))
      val vars        = usedVars.map(field => s"${field._1}: ${field._2}").mkString(", ")
      val queryHeader = if (usedVars.isEmpty) "query" else s"query ($vars) "
      queryHeader + query
    }
    val prependedQuery = prependQueryWithHeader(query)
    isQueryValidGraphQL(prependedQuery) match {
      case None =>
        isQueryValidGraphQL(query) match {
          case None      => "# Could not parse the query. Please check that it is valid.\n" + query
          case Some(doc) => renderQueryWithCorrectNameWithSangria(doc)
        }
      case Some(doc) => renderQueryWithCorrectNameWithSangria(doc)
    }
  }

  def isQueryValidGraphQL(query: String): Option[Document] = sangria.parser.QueryParser.parse(query).toOption

  def validatePermissionQuery(query: String, project: Project)(implicit inj: Injector): Vector[Violation] = {

    val permissionSchema: Schema[UserContext, Unit] = PermissionSchemaResolver.permissionSchema(project)
    sangria.parser.QueryParser.parse(query).toOption match {
      case None      => sys.error("could not even parse the query")
      case Some(doc) => QueryValidator.default.validateQuery(permissionSchema, doc)
    }
  }

  def bundleQueriesInOneFile(queries: Seq[String], name: String): Option[FileContainer] = {
    val fileContainer = queries.isEmpty match {
      case true  => None
      case false => Some(FileContainer(path = s"$name.graphql", content = queries.distinct.mkString("\n")))
    }
    fileContainer
  }

  /** Creates the fileContainer whose content will be stored in the backend
    * Ensures that the query is valid GraphQL and will set the name to ruleName if one exists
    * Will error on invalid GraphQL
    */
  def fileContainerFromQueryPath(inputPath: Option[String], files: Map[String, String]): Option[FileContainer] = {
    inputPath match {
      case Some(path) =>
        splitPathInRuleNameAndPath(path) match {
          case (Some(name), Some(pathPart)) =>
            Some(FileContainer(path, renderQueryForName(name, pathPart, files)))

          case (None, Some(pathPart)) =>
            isQueryValidGraphQL(files(pathPart)) match {
              case None      => throw QueryPermissionParseError("noName", s"Query could not be parsed. Please ensure it is valid GraphQL. ${files(pathPart)}")
              case Some(doc) => Some(FileContainer(pathPart, QueryRenderer.render(doc))) // todo take out comments too here
            }
          case _ => sys.error("This should not happen.")
        }
      case None =>
        None
    }
  }

  def astPermissionWithoutId(permission: AstPermissionWithAllInfos): AstPermissionWithAllInfos = permission.copy(permissionId = "")

  def generateAlternativeRuleName(otherPermissionsWithSameOperationIds: List[String], permissionId: String, operation: String): String = {
    val sortedOtherPermissions = otherPermissionsWithSameOperationIds.sorted
    val ownIndex               = sortedOtherPermissions.indexOf(permissionId)
    alternativeNameFromOperationAndInt(operation, ownIndex)
  }

  def alternativeNameFromOperationAndInt(operation: String, ownIndex: Int): String = {
    ownIndex match {
      case 0 => operation
      case x => s"$operation${x + 1}"
    }
  }

  def astPermissionsWithAllInfosFromProject(project: Project): Vector[AstPermissionWithAllInfos] = {

    val modelPermissions = project.models.flatMap { model =>
      model.permissions.filter(_.isActive).map { permission =>
        val astPermission = Ast.Permission(
          description = permission.description,
          operation = s"${model.name}.${permission.operationString}",
          authenticated = permission.userType == UserType.Authenticated,
          queryPath = permission.ruleGraphQueryFilePath,
          fields = if (permission.applyToWholeModel) {
            None
          } else {
            Some(permission.fieldIds.toVector.map(id => model.getFieldById_!(id).name))
          }
        )
        AstPermissionWithAllInfos(astPermission, permission.ruleGraphQuery, permission.ruleGraphQueryFilePath, permission.id)
      }
    }.toVector

    val relationPermissions = project.relations.flatMap { relation =>
      relation.permissions
        .filter(_.isActive)
        .map { permission =>
          val astPermission = Ast.Permission(
            description = permission.description,
            operation = s"${relation.name}.${permission.operation}",
            authenticated = permission.userType == UserType.Authenticated,
            queryPath = permission.ruleGraphQueryFilePath
          )

          AstPermissionWithAllInfos(astPermission, permission.ruleGraphQuery, permission.ruleGraphQueryFilePath, permission.id)
        }
        .toVector

    }
    modelPermissions ++ relationPermissions
  }

}
