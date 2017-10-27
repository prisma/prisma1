package cool.graph.system.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph._
import cool.graph.client.database.DataResolver
import cool.graph.metrics.CounterMetric
import cool.graph.shared.database.{InternalAndProjectDbs, InternalDatabase}
import cool.graph.shared.errors.SystemErrors.{SchemaError, WithSchemaError}
import cool.graph.shared.functions.ExternalFile
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.system.database.client.ClientDbQueries
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.metrics.SystemMetrics
import cool.graph.system.migration.ProjectConfig.Ast
import cool.graph.system.migration.dataSchema._
import cool.graph.system.migration.dataSchema.validation.{SchemaErrors, SchemaValidator}
import cool.graph.system.migration.project.{ClientInterchange, ClientInterchangeFormatModule}
import cool.graph.system.migration.{ModuleActions, ModuleMigrator, ProjectConfig}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateProject, UpdateTypeAndFieldPositions}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.collection.{Seq, immutable}
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

case class PushMutation(
    client: Client,
    project: Project,
    args: PushInput,
    dataResolver: DataResolver,
    projectDbsFn: models.Project => InternalAndProjectDbs,
    clientDbQueries: ClientDbQueries
)(implicit inj: Injector)
    extends InternalProjectMutation[PushMutationPayload]
    with Injectable {
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val projectQueries: ProjectQueries           = inject[ProjectQueries](identified by "projectQueries")

  var verbalDescriptions: Seq[VerbalDescription] = Seq.empty
  var errors: Seq[SchemaError]                   = Seq.empty

  override def prepareActions(): List[Mutaction] = {
    PushMutation.pushMutationCount.incBy(1)

    project.isEjected match {
      case false =>
        errors = List(
          SchemaError(
            "Global",
            "Only projects that have been ejected can make use of the CLI's deploy function. More details: https://docs-next.graph.cool/reference/service-definition/legacy-console-projects-aemieb1aev"
          ))
        actions

      case true =>
        DeployMutactions.generate(args.config, args.force, args.isDryRun, client, project, internalDatabase, clientDbQueries, projectQueries) match {
          case Success(result) =>
            actions = result.mutactions.toList
            verbalDescriptions = result.verbalDescriptions
            errors = result.errors
            PushMutation.pushMutationMutactionsCount.incBy(this.actions.length)

          case Failure(error) =>
            actions = List.empty
            verbalDescriptions = List.empty
            errors = List(extractErrors(error))
        }
        actions
    }
  }

  override def verifyActions(): Future[List[Try[MutactionVerificationSuccess]]] = {
    super.verifyActions().map { verifications =>
      verifications.map {
        case Failure(error: Throwable) =>
          errors = errors :+ extractErrors(error)
          verbalDescriptions = List.empty
          actions = List.empty
          Success(MutactionVerificationSuccess())

        case verification =>
          verification
      }
    }
  }

  def extractErrors(exc: Throwable): SchemaError = exc match {
    case sysError: WithSchemaError =>
      val fallbackError = SchemaError.global(sysError.getMessage)
      sysError.schemaError.getOrElse(fallbackError)
    case e: Throwable =>
      SchemaError.global(e.getMessage)
  }

  override def performActions(requestContext: Option[SystemRequestContextTrait]): Future[List[MutactionExecutionResult]] = {
    if (args.isDryRun) {
      Future.successful(List(MutactionExecutionSuccess()))
    } else {
      super.performActions(requestContext)
    }
  }

  override def getReturnValue: Option[PushMutationPayload] = {
    Some(
      PushMutationPayload(
        clientMutationId = args.clientMutationId,
        client = client, //.copy(projects = client.projects :+ newProject),
        project = project,
        verbalDescriptions = verbalDescriptions,
        errors = errors
      )
    )
  }
}

object PushMutation {

  val pushMutationMutactionsCount: CounterMetric = SystemMetrics.defineCounter("pushMutationMutactionsCount")
  val pushMutationCount: CounterMetric           = SystemMetrics.defineCounter("pushMutationCount")

}

object DeployMutactions {

  case class DeployResult(mutactions: Vector[Mutaction], verbalDescriptions: Vector[VerbalDescription], errors: Vector[SchemaError])

  def generate(config: String,
               force: Boolean,
               isDryRun: Boolean,
               client: Client,
               project: Project,
               internalDatabase: InternalDatabase,
               clientDbQueries: ClientDbQueries,
               projectQueries: ProjectQueries)(implicit inj: Injector, system: ActorSystem, materializer: ActorMaterializer): Try[DeployResult] = Try {
    var verbalDescriptions: Vector[VerbalDescription] = Vector.empty
    var errors: Vector[SchemaError]                   = Vector.empty
    var mutactions: Vector[Mutaction]                 = Vector.empty[Mutaction]
    var currentProject: Option[Project]               = None

    val (combinedFileMap: Map[String, String], externalFilesMap: Option[Map[String, ExternalFile]], combinedParsedModules: Seq[Ast.Module]) =
      combineAllModulesIntoOne(config)

    val moduleMigratorBeforeSchemaChanges: ModuleMigrator =
      ModuleMigrator(client, project, combinedParsedModules, combinedFileMap, externalFilesMap, isDryRun = isDryRun)
    val combinedSchema: String = moduleMigratorBeforeSchemaChanges.schemaContent

    val schemaFileHeader: SchemaFileHeader = SchemaFileHeader(projectId = project.id, version = project.revision)

    def getProject = currentProject.getOrElse(project)

    def runMigrator(function: => ModuleActions) = {
      val moduleActions         = function
      val (mutations, cProject) = moduleActions.determineMutations(client, getProject, _ => InternalAndProjectDbs(internalDatabase))
      verbalDescriptions ++= moduleActions.verbalDescriptions
      mutactions ++= mutations.toList.flatMap(_.prepareActions())
      currentProject = Some(cProject)
    }

    //Delete Permissions, Functions and RootTokens
    runMigrator(moduleMigratorBeforeSchemaChanges.determineActionsForRemove)

    // Update SCHEMA
    errors ++= SchemaValidator(getProject, combinedSchema, schemaFileHeader).validate()
    if (errors.nonEmpty) {
      return Success(DeployResult(mutactions = Vector.empty, verbalDescriptions = Vector.empty, errors = errors))
    }

    val schemaMigrator               = SchemaMigrator(getProject, combinedSchema, None)
    val actions: UpdateSchemaActions = schemaMigrator.determineActionsForUpdate()

    verbalDescriptions ++= actions.verbalDescriptions

    if (actions.isDestructive && !force && !isDryRun) {
      return Success(
        DeployResult(mutactions = Vector.empty, verbalDescriptions = Vector.empty, errors = Vector[SchemaError](SchemaErrors.forceArgumentRequired)))
    }

    val (mutations, cProject) = actions.determineMutations(client, getProject, _ => InternalAndProjectDbs(internalDatabase), clientDbQueries)

    currentProject = Some(cProject)

    // UPDATE PROJECT
    val updateTypeAndFieldPositions = UpdateTypeAndFieldPositions(
      project = getProject,
      client = client,
      newSchema = schemaMigrator.diffResult.newSchema,
      internalDatabase = internalDatabase.databaseDef,
      projectQueries = projectQueries
    )

    mutactions ++= mutations.toVector.flatMap(_.prepareActions()) ++ Vector(updateTypeAndFieldPositions)

    // Add Functions, Permissions and RootTokens
    val moduleMigratorAfterSchemaChanges =
      ModuleMigrator(client, getProject, combinedParsedModules, combinedFileMap, externalFilesMap, isDryRun = isDryRun, afterSchemaMigration = true)

    runMigrator(moduleMigratorAfterSchemaChanges.determineActionsForAdd)

    //Update Functions
    runMigrator(moduleMigratorAfterSchemaChanges.determineActionsForUpdate)

    if (errors.isEmpty) {
      val shouldBump               = mutactions.exists(_.isInstanceOf[BumpProjectRevision])
      val setsGlobalStarPermission = moduleMigratorAfterSchemaChanges.permissionDiff.containsGlobalStarPermission
      val hasChanged               = project.hasGlobalStarPermission != setsGlobalStarPermission
      val setGlobalStarPermissionMutaction = UpdateProject(
        client = client,
        oldProject = project,
        project = getProject.copy(hasGlobalStarPermission = setsGlobalStarPermission),
        internalDatabase = internalDatabase.databaseDef,
        projectQueries = projectQueries,
        bumpRevision = shouldBump
      )
      mutactions ++= Vector(setGlobalStarPermissionMutaction)
      if (hasChanged) {
        if (setsGlobalStarPermission) {
          verbalDescriptions ++= Vector(
            VerbalDescription(
              `type` = "permission",
              action = "Create",
              name = "Wildcard Permission",
              description = s"The wildcard permission for all types is added."
            ))
        } else {
          verbalDescriptions ++= Vector(
            VerbalDescription(
              `type` = "permission",
              action = "Delete",
              name = "Wildcard Permission",
              description = s"The wildcard permission for all types is removed."
            ))
        }
      }
    }

    val shouldBump         = mutactions.exists(_.isInstanceOf[BumpProjectRevision])
    val shouldInvalidate   = mutactions.exists(_.isInstanceOf[InvalidateSchema])
    val finalProject       = currentProject.getOrElse(project)
    val filteredMutactions = mutactions.filter(mutaction => !mutaction.isInstanceOf[BumpProjectRevision] && !mutaction.isInstanceOf[InvalidateSchema])

    val invalidateSchemaAndBumpRevisionIfNecessary =
      (errors.isEmpty, shouldBump, shouldInvalidate) match {
        case (false, _, _)        => List.empty
        case (true, false, false) => List.empty
        case (true, true, true)   => List(BumpProjectRevision(finalProject), InvalidateSchema(finalProject))
        case (true, true, false)  => List(BumpProjectRevision(finalProject))
        case (true, false, true)  => List(InvalidateSchema(finalProject))
      }
    val finalMutactions = filteredMutactions ++ invalidateSchemaAndBumpRevisionIfNecessary

    DeployResult(mutactions = finalMutactions, verbalDescriptions = verbalDescriptions, errors = errors)
  }

  private def combineAllModulesIntoOne(config: String): (Map[String, String], Option[Map[String, ExternalFile]], Seq[Ast.Module]) = {
    val modules: immutable.Seq[ClientInterchangeFormatModule] = ClientInterchange.parse(config).modules
    val rootModule: ClientInterchangeFormatModule             = modules.find(_.name == "").getOrElse(throw sys.error("There needs to be a root module with name \"\" "))
    val parsedRootModule: Ast.Module                          = ProjectConfig.parse(rootModule.content)

    val (prependedParsedNonRootModules, prependedNonRootModulesFiles) = parsedRootModule.modules match {
      case Some(modulesMap) =>
        val nonRootModules: immutable.Seq[ClientInterchangeFormatModule] = modules.filter(_.name != "")
        val parsedModuleAndFilesTuplesList = nonRootModules.map { module =>
          val pathFromRoot = createPathFromRoot(modulesMap, module)

          val parsedModule: Ast.Module = ProjectConfig.parse(module.content)

          val prependedTypes = parsedModule.types.map(path => pathFromRoot + path.drop(1))

          val prependedPermissions: Seq[Ast.Permission] =
            parsedModule.permissions.map(permission => permission.copy(queryPath = permission.queryPath.map(path => pathFromRoot + path.drop(1))))

          val prependedSchemaPathFunctions: Map[String, Ast.Function] = parsedModule.functions.map {
            case (x, function) => (x, function.copy(schema = function.schema.map(path => pathFromRoot + path.drop(1))))
          }

          val prependedQueryAndSchemaPathFunctions = prependedSchemaPathFunctions.map {
            case (x, function) => (x, function.copy(query = function.query.map(path => pathFromRoot + path.drop(1))))
          }

          val prependedCodeAndQueryAndSchemaPathFunctions = prependedQueryAndSchemaPathFunctions.map {
            case (x, function) =>
              (x, function.copy(handler = function.handler.copy(code = function.handler.code.map(code => code.copy(src = pathFromRoot + code.src.drop(1))))))
          }

          val prependedAndParsedModule: Ast.Module =
            parsedModule.copy(
              types = prependedTypes,
              permissions = prependedPermissions.toVector,
              functions = prependedCodeAndQueryAndSchemaPathFunctions,
              rootTokens = parsedModule.rootTokens
            )

          val prependedFile: Map[String, String] = module.files.map { case (key, value) => (pathFromRoot ++ key.drop(1), value) }

          (prependedAndParsedModule, prependedFile)
        }

        parsedModuleAndFilesTuplesList.unzip

      case None =>
        (Seq.empty, Seq.empty)
    }

    val combinedFileMap: Map[String, String] = prependedNonRootModulesFiles.foldLeft(rootModule.files)(_ ++ _)
    val combinedParsedModules                = parsedRootModule +: prependedParsedNonRootModules

    val externalFilesMap = modules.headOption.flatMap(module => {
      module.externalFiles
    })

    (combinedFileMap, externalFilesMap, combinedParsedModules)
  }

  private def createPathFromRoot(modulesMap: Map[String, String], module: ClientInterchangeFormatModule) = {
    val modulepath = modulesMap(module.name)
    val lastSlash  = modulepath.lastIndexOf("/")
    modulepath.slice(0, lastSlash)
  }
}

case class PushMutationPayload(clientMutationId: Option[String],
                               client: models.Client,
                               project: models.Project,
                               verbalDescriptions: Seq[VerbalDescription],
                               errors: Seq[SchemaError])
    extends Mutation

case class PushInput(clientMutationId: Option[String], config: String, projectId: String, version: Int, isDryRun: Boolean, force: Boolean)
