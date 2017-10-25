package cool.graph.system

import akka.actor.ActorSystem
import cool.graph.InternalMutation
import cool.graph.shared.database.{GlobalDatabaseManager, InternalAndProjectDbs, InternalDatabase}
import cool.graph.shared.errors.SystemErrors
import cool.graph.shared.errors.SystemErrors.InvalidProjectId
import cool.graph.shared.functions.FunctionEnvironment
import cool.graph.shared.models
import cool.graph.shared.models.{Client, Project, ProjectDatabase, ProjectWithClientId}
import cool.graph.system.authorization.SystemAuth
import cool.graph.system.database.client.{ClientDbQueries, ClientDbQueriesImpl}
import cool.graph.system.database.finder.{ProjectFinder, ProjectResolver}
import cool.graph.system.mutations._
import cool.graph.system.schema.fields._
import cool.graph.system.schema.types._
import sangria.relay.{Connection, ConnectionDefinition, Edge, Mutation}
import sangria.schema.{Field, ObjectType, OptionType, Schema, StringType, UpdateCtx, fields}
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

trait SchemaBuilder {
  def apply(userContext: SystemUserContext): Schema[SystemUserContext, Unit]
}

object SchemaBuilder {
  def apply(fn: SystemUserContext => Schema[SystemUserContext, Unit]): SchemaBuilder = new SchemaBuilder {
    override def apply(userContext: SystemUserContext) = fn(userContext)
  }
}

class SchemaBuilderImpl(
    userContext: SystemUserContext,
    globalDatabaseManager: GlobalDatabaseManager,
    internalDatabase: InternalDatabase
)(
    implicit inj: Injector,
    system: ActorSystem
) extends Injectable {
  import system.dispatcher

  implicit val projectResolver: ProjectResolver                   = inject[ProjectResolver](identified by "projectResolver")
  val functionEnvironment                                         = inject[FunctionEnvironment]
  lazy val clientType: ObjectType[SystemUserContext, Client]      = Customer.getType(userContext.clientId)
  lazy val viewerType: ObjectType[SystemUserContext, ViewerModel] = Viewer.getType(clientType, projectResolver)
  lazy val ConnectionDefinition(clientEdge, _)                    = Connection.definition[SystemUserContext, Connection, models.Client]("Client", clientType)

  def build(): Schema[SystemUserContext, Unit] = {
    val Query = ObjectType(
      "Query",
      viewerField() :: nodeField :: Nil
    )

    val Mutation = ObjectType(
      "Mutation",
      getFields.toList
    )

    Schema(Query, Some(Mutation))
  }

  def viewerField(): Field[SystemUserContext, Unit] = Field(
    "viewer",
    fieldType = viewerType,
    resolve = _ => ViewerModel()
  )

  def getFields: Vector[Field[SystemUserContext, Unit]] = Vector(
    getPushField,
    getTemporaryDeployUrl,
    getAddProjectField,
    getAuthenticateCustomerField,
    getExportDataField,
    getGenerateNodeTokenMutationField
  )

  def getPushField: Field[SystemUserContext, Unit] = {
    import Push.fromInput

    Mutation
      .fieldWithClientMutationId[SystemUserContext, Unit, PushMutationPayload, PushInput](
        fieldName = "push",
        typeName = "Push",
        inputFields = Push.inputFields,
        outputFields = fields(
          Field("project", OptionType(ProjectType), resolve = ctx => ctx.value.project),
          Field("migrationMessages", VerbalDescriptionType.TheListType, resolve = ctx => ctx.value.verbalDescriptions),
          Field("errors", SchemaErrorType.TheListType, resolve = ctx => ctx.value.errors)
        ),
        mutateAndGetPayload = (input, ctx) =>
          UpdateCtx({

            for {
              project <- getProjectOrThrow(input.projectId)
              mutator = PushMutation(
                client = ctx.ctx.getClient,
                project = project.project,
                args = input,
                dataResolver = ctx.ctx.dataResolver(project.project),
                projectDbsFn = internalAndProjectDbsForProject,
                clientDbQueries = clientDbQueries(project.project)
              )
              payload <- mutator.run(ctx.ctx).flatMap { payload =>
                          val clientId = ctx.ctx.getClient.id
                          ProjectFinder
                            .loadById(clientId, payload.project.id)
                            .map(project => payload.copy(project = project))
                        }
            } yield {
              payload
            }
          }) { payload =>
            ctx.ctx.refresh()
        }
      )
  }

  def getTemporaryDeployUrl: Field[SystemUserContext, Unit] = {
    import GetTemporaryDeploymentUrl.fromInput

    case class GetTemporaryDeployUrlPayload(url: String, clientMutationId: Option[String] = None) extends Mutation

    Mutation
      .fieldWithClientMutationId[SystemUserContext, Unit, GetTemporaryDeployUrlPayload, GetTemporaryDeployUrlInput](
        fieldName = "getTemporaryDeployUrl",
        typeName = "GetTemporaryDeployUrl",
        inputFields = GetTemporaryDeploymentUrl.inputFields,
        outputFields = fields(
          Field("url", StringType, resolve = ctx => ctx.value.url)
        ),
        mutateAndGetPayload = (input, ctx) =>
          for {
            project      <- getProjectOrThrow(input.projectId)
            temporaryUrl <- functionEnvironment.getTemporaryUploadUrl(project.project)
          } yield {
            GetTemporaryDeployUrlPayload(temporaryUrl, None)
        }
      )
  }

  def getAddProjectField: Field[SystemUserContext, Unit] = {
    import AddProject.manual

    Mutation.fieldWithClientMutationId[SystemUserContext, Unit, AddProjectMutationPayload, AddProjectInput](
      fieldName = "addProject",
      typeName = "AddProject",
      inputFields = AddProject.inputFields,
      outputFields = fields(
        Field("viewer", viewerType, resolve = _ => ViewerModel()),
        Field("project", ProjectType, resolve = ctx => ctx.value.project),
        Field("user", clientType, resolve = ctx => ctx.ctx.getClient),
        Field("projectEdge", projectEdge, resolve = ctx => Edge(node = ctx.value.project, cursor = Connection.offsetToCursor(0))),
        Field("migrationMessages", VerbalDescriptionType.TheListType, resolve = ctx => ctx.value.verbalDescriptions),
        Field("errors", SchemaErrorType.TheListType, resolve = ctx => ctx.value.errors)
      ),
      mutateAndGetPayload = (input, ctx) =>
        UpdateCtx({
          val mutator = new AddProjectMutation(
            client = ctx.ctx.getClient,
            args = input,
            projectDbsFn = internalAndProjectDbsForProject,
            internalDatabase = internalDatabase,
            globalDatabaseManager = globalDatabaseManager
          )

          mutator
            .run(ctx.ctx)
            .flatMap(payload => {
              val clientId = ctx.ctx.getClient.id
              ProjectFinder
                .loadById(clientId, payload.project.id)
                .map(project => payload.copy(project = project))
            })

        }) { payload =>
          ctx.ctx.refresh()
      }
    )
  }

  def getAuthenticateCustomerField: Field[SystemUserContext, Unit] = {
    import AuthenticateCustomer.manual

    Mutation
      .fieldWithClientMutationId[SystemUserContext, Unit, AuthenticateCustomerMutationPayload, AuthenticateCustomerInput](
        fieldName = "authenticateCustomer",
        typeName = "AuthenticateCustomer",
        inputFields = AuthenticateCustomer.inputFields,
        outputFields = fields(
          Field("viewer", viewerType, resolve = _ => ViewerModel()),
          Field("user", clientType, resolve = ctx => ctx.ctx.getClient),
          Field("userEdge", clientEdge, resolve = ctx => Edge(node = ctx.ctx.getClient, cursor = Connection.offsetToCursor(0))),
          Field("token", StringType, resolve = ctx => {
            val auth = new SystemAuth()
            auth.generateSessionToken(ctx.value.client.id)
          })
        ),
        mutateAndGetPayload = (input, ctx) =>
          UpdateCtx({

            ctx.ctx.auth
              .loginByAuth0IdToken(input.auth0IdToken)
              .flatMap {
                case Some((sessionToken, id)) =>
                  val userContext = ctx.ctx.refresh(Some(id))
                  Future.successful(AuthenticateCustomerMutationPayload(input.clientMutationId, userContext.client.get))
                case None =>
                  val mutator = createAuthenticateCustomerMutation(input)
                  mutator.run(ctx.ctx)

              }
          }) { payload =>
            ctx.ctx.refresh(Some(payload.client.id))
        }
      )
  }

  def createAuthenticateCustomerMutation(input: AuthenticateCustomerInput): InternalMutation[AuthenticateCustomerMutationPayload] = {
    AuthenticateCustomerMutation(
      args = input,
      internalDatabase = internalDatabase,
      projectDbsFn = internalAndProjectDbsForProjectDatabase
    )
  }

  def getExportDataField: Field[SystemUserContext, Unit] = {
    import ExportData.manual

    Mutation
      .fieldWithClientMutationId[SystemUserContext, Unit, ExportDataMutationPayload, ExportDataInput](
        fieldName = "exportData",
        typeName = "ExportData",
        inputFields = ExportData.inputFields,
        outputFields = fields(
          Field("viewer", viewerType, resolve = _ => ViewerModel()),
          Field("project", ProjectType, resolve = ctx => ctx.value.project),
          Field("user", clientType, resolve = ctx => ctx.ctx.getClient),
          Field("url", StringType, resolve = ctx => ctx.value.url)
        ),
        mutateAndGetPayload = (input, ctx) => {
          val project = ProjectFinder.loadById(ctx.ctx.getClient.id, input.projectId)
          project.flatMap { project =>
            val mutator = ExportDataMutation(
              client = ctx.ctx.getClient,
              project = project,
              args = input,
              projectDbsFn = internalAndProjectDbsForProject,
              dataResolver = ctx.ctx.dataResolver(project)
            )

            mutator
              .run(ctx.ctx)
              .flatMap { payload =>
                val clientId = ctx.ctx.getClient.id
                ProjectFinder
                  .loadById(clientId, payload.project.id)
                  .map(project => payload.copy(project = project))
              }
          }
        }
      )
  }

  def getGenerateNodeTokenMutationField: Field[SystemUserContext, Unit] = {
    import cool.graph.system.schema.fields.GenerateNodeToken.manual

    Mutation
      .fieldWithClientMutationId[SystemUserContext, Unit, GenerateUserTokenPayload, GenerateUserTokenInput](
        fieldName = "generateNodeToken",
        typeName = "GenerateNodeToken",
        inputFields = cool.graph.system.schema.fields.GenerateNodeToken.inputFields,
        outputFields = fields(Field("token", StringType, resolve = ctx => ctx.value.token)),
        mutateAndGetPayload = (input, ctx) => {
          projectResolver
            .resolve(input.projectId)
            .flatMap {
              case Some(project) =>
                val mutation = mutations.GenerateUserToken(project = project, args = input, projectDbsFn = internalAndProjectDbsForProject)
                mutation.run(ctx.ctx)
              case _ =>
                throw SystemErrors.InvalidProjectId(projectId = input.projectId)
            }
        }
      )
  }

  def internalAndProjectDbsForProjectDatabase(projectDatabase: ProjectDatabase): InternalAndProjectDbs = {
    val clientDbs = globalDatabaseManager.getDbForProjectDatabase(projectDatabase)
    InternalAndProjectDbs(internalDatabase, clientDbs)
  }

  def internalAndProjectDbsForProject(project: Project): InternalAndProjectDbs = {
    val clientDbs = globalDatabaseManager.getDbForProject(project)
    InternalAndProjectDbs(internalDatabase, clientDbs)
  }

  def clientDbQueries(project: Project): ClientDbQueries = ClientDbQueriesImpl(globalDatabaseManager)(project)

  def getProjectOrThrow(projectId: String): Future[ProjectWithClientId] = {
    projectResolver.resolveProjectWithClientId(projectIdOrAlias = projectId).map { projectOpt =>
      projectOpt.getOrElse(throw InvalidProjectId(projectId))
    }
  }
}
