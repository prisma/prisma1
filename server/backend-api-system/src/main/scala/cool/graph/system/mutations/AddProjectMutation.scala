package cool.graph.system.mutations

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.cuid.Cuid
import cool.graph.shared.database.{GlobalDatabaseManager, InternalAndProjectDbs, InternalDatabase}
import cool.graph.shared.errors.SystemErrors.{InvalidProjectDatabase, SchemaError, WithSchemaError}
import cool.graph.shared.errors.UserInputErrors.InvalidSchema
import cool.graph.shared.models
import cool.graph.shared.models.Region.Region
import cool.graph.shared.models._
import cool.graph.system.database.client.ClientDbQueriesImpl
import cool.graph.system.database.finder.{ProjectDatabaseFinder, ProjectQueries}
import cool.graph.system.migration.dataSchema._
import cool.graph.system.migration.dataSchema.validation.SchemaSyntaxValidator
import cool.graph.system.mutactions.internal.{InvalidateSchema, UpdateTypeAndFieldPositions}
import cool.graph.{InternalMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

import scala.collection.Seq
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success}

case class AddProjectMutation(
    client: Client,
    args: AddProjectInput,
    internalDatabase: InternalDatabase,
    projectDbsFn: Project => InternalAndProjectDbs,
    globalDatabaseManager: GlobalDatabaseManager
)(implicit inj: Injector)
    extends InternalMutation[AddProjectMutationPayload]
    with Injectable {

  implicit val system: ActorSystem             = inject[ActorSystem](identified by "actorSystem")
  implicit val materializer: ActorMaterializer = inject[ActorMaterializer](identified by "actorMaterializer")
  val projectQueries: ProjectQueries           = inject[ProjectQueries](identified by "projectQueries")

  val projectDatabaseFuture: Future[Option[ProjectDatabase]] = args.projectDatabaseId match {
    case Some(id) => ProjectDatabaseFinder.forId(id)(internalDatabase.databaseDef)
    case None     => ProjectDatabaseFinder.defaultForRegion(args.region)(internalDatabase.databaseDef)
  }

  val projectDatabase: ProjectDatabase = Await.result(projectDatabaseFuture, 5.seconds) match {
    case Some(db) => db
    case None     => throw InvalidProjectDatabase(args.projectDatabaseId.getOrElse(args.region.toString))
  }

  val newProject: Project = AddProjectMutation.base(
    name = args.name,
    alias = args.alias,
    client = client,
    projectDatabase = projectDatabase,
    isEjected = args.config.nonEmpty
  )

  var verbalDescriptions: Seq[VerbalDescription] = Seq.empty
  var errors: Seq[SchemaError]                   = Seq.empty

  override val databases: InternalAndProjectDbs = projectDbsFn(newProject)

  override def prepareActions(): List[Mutaction] = {
    actions ++= AuthenticateCustomerMutation.createInternalStructureForNewProject(client,
                                                                                  newProject,
                                                                                  projectQueries = projectQueries,
                                                                                  internalDatabase.databaseDef)
    actions ++= AuthenticateCustomerMutation.createClientDatabaseStructureForNewProject(client, newProject, internalDatabase.databaseDef)
    actions ++= AuthenticateCustomerMutation.createIntegrationsForNewProject(newProject)

    val actionsForSchema: List[Mutaction] = args.schema match {
      case Some(schema) => initActionsForSchemaFile(schema)
      case None         => List.empty
    }

    actions ++= actionsForSchema

    args.config match {
      case Some(config) =>
        val clientDbQueries = ClientDbQueriesImpl(globalDatabaseManager)(newProject)
        val deployResult = DeployMutactions.generate(
          config,
          force = true,
          isDryRun = false,
          client = client,
          project = newProject,
          internalDatabase = internalDatabase,
          clientDbQueries = clientDbQueries,
          projectQueries = projectQueries
        )

        def extractErrors(exc: Throwable): SchemaError = exc match {
          case sysError: WithSchemaError =>
            val fallbackError = SchemaError.global(sysError.getMessage)
            sysError.schemaError.getOrElse(fallbackError)
          case e: Throwable =>
            SchemaError.global(e.getMessage)
        }

        deployResult match {
          case Success(result) =>
            actions ++= result.mutactions.toList
            verbalDescriptions ++= result.verbalDescriptions
            errors ++= result.errors

          case Failure(error) =>
            actions = List.empty
            verbalDescriptions = List.empty
            errors = List(extractErrors(error))
        }
      case None => ()
    }

    actions :+= InvalidateSchema(project = newProject)
    actions
  }

  def initActionsForSchemaFile(schema: String): List[Mutaction] = {
    val errors = SchemaSyntaxValidator(schema).validate()
    if (errors.nonEmpty) {
      val message = errors.foldLeft("") { (acc, error) =>
        acc + "\n  " + error.description
      }
      throw InvalidSchema(message)
    }

    val migrator  = SchemaMigrator(newProject, schema, args.clientMutationId)
    val mutations = migrator.determineActionsForInit().determineMutations(client, newProject, _ => InternalAndProjectDbs(internalDatabase))

    val updateTypeAndFieldPositions = UpdateTypeAndFieldPositions(
      project = newProject,
      client = client,
      newSchema = migrator.diffResult.newSchema,
      internalDatabase = internalDatabase.databaseDef,
      projectQueries = projectQueries
    )

    mutations.toList.flatMap(_.prepareActions()) :+ updateTypeAndFieldPositions
  }

  override def getReturnValue: Option[AddProjectMutationPayload] = {
    Some(
      AddProjectMutationPayload(
        clientMutationId = args.clientMutationId,
        client = client.copy(projects = client.projects :+ newProject),
        project = newProject,
        verbalDescriptions = verbalDescriptions,
        errors = errors
      )
    )
  }
}

case class AddProjectMutationPayload(clientMutationId: Option[String],
                                     client: models.Client,
                                     project: models.Project,
                                     verbalDescriptions: Seq[VerbalDescription],
                                     errors: Seq[SchemaError])
    extends Mutation

case class AddProjectInput(clientMutationId: Option[String],
                           name: String,
                           alias: Option[String],
                           webhookUrl: Option[String],
                           schema: Option[String],
                           region: Region = Region.EU_WEST_1,
                           projectDatabaseId: Option[String],
                           config: Option[String])

object AddProjectMutation {
  def base(name: String, alias: Option[String], client: Client, projectDatabase: ProjectDatabase, isEjected: Boolean): Project = {
    val predefinedModels = if (isEjected) {
      Vector.empty
    } else {
      val generatedUserFields = SignupCustomerMutation.generateUserFields
      val userModel           = SignupCustomerMutation.generateUserModel.copy(fields = generatedUserFields)

      val generatedFileFields = SignupCustomerMutation.generateFileFields
      val fileModel           = SignupCustomerMutation.generateFileModel.copy(fields = generatedFileFields)

      Vector(userModel, fileModel)
    }

    models.Project(
      id = Cuid.createCuid(),
      alias = alias,
      name = name,
      webhookUrl = None,
      models = predefinedModels.toList,
      relations = List.empty,
      actions = List.empty,
      ownerId = client.id,
      projectDatabase = projectDatabase,
      isEjected = isEjected,
      revision = if (isEjected) 0 else 1
    )
  }
}
