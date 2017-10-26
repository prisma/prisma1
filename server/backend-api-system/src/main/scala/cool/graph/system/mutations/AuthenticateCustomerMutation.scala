package cool.graph.system.mutations

import com.typesafe.config.Config
import cool.graph._
import cool.graph.client.database.DatabaseMutationBuilder
import cool.graph.cuid.Cuid
import cool.graph.shared.database.{InternalAndProjectDbs, InternalDatabase}
import cool.graph.shared.models
import cool.graph.shared.models.CustomerSource.CustomerSource
import cool.graph.shared.models._
import cool.graph.system.authorization.SystemAuth
import cool.graph.system.database.SystemFields
import cool.graph.system.database.client.EmptyClientDbQueries
import cool.graph.system.database.finder.ProjectQueries
import cool.graph.system.mutactions.client.{CreateClientDatabaseForProject, CreateColumn, CreateModelTable, CreateRelationTable}
import cool.graph.system.mutactions.internal._
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.ExecutionContextExecutor

case class AuthenticateCustomerMutation(
    args: AuthenticateCustomerInput,
    internalDatabase: InternalDatabase,
    projectDbsFn: ProjectDatabase => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalMutation[AuthenticateCustomerMutationPayload]
    with Injectable {

  val internalDatabaseDef     = internalDatabase.databaseDef
  implicit val config: Config = inject[Config](identified by "config")
  val projectQueries          = inject[ProjectQueries](identified by "projectQueries")

  var newClient: Option[models.Client]   = None
  var newProject: Option[models.Project] = None

  val projectDatabase: ProjectDatabase          = DefaultProjectDatabase.blocking(internalDatabaseDef)
  override val databases: InternalAndProjectDbs = projectDbsFn(projectDatabase)

  override def prepareActions(): List[Mutaction] = {

    val auth = new SystemAuth()

    val idTokenData = auth.parseAuth0IdToken(args.auth0IdToken).get

    val name =
      idTokenData.user_metadata.map(_.name).getOrElse(idTokenData.name)

    val (actions, client, project) = AuthenticateCustomerMutation.generateActions(
      name = name,
      auth0Id = idTokenData.sub,
      email = idTokenData.email,
      source = CustomerSource.HOMEPAGE,
      internalDatabase = internalDatabaseDef,
      projectQueries = projectQueries,
      projectDatabase = projectDatabase
    )
    this.actions = actions
    this.newClient = Some(client)
    this.newProject = Some(project)

    actions
  }

  override def getReturnValue(): Option[AuthenticateCustomerMutationPayload] = {

    val auth         = new SystemAuth()
    val sessionToken = auth.generateSessionToken(newClient.get.id)

    Some(AuthenticateCustomerMutationPayload(clientMutationId = args.clientMutationId, client = newClient.get))
  }
}

case class AuthenticateCustomerMutationPayload(clientMutationId: Option[String], client: models.Client) extends Mutation

case class AuthenticateCustomerInput(clientMutationId: Option[String], auth0IdToken: String)

object AuthenticateCustomerMutation {
  def generateUserModel = {
    Model(
      id = Cuid.createCuid(),
      name = "User",
      isSystem = true,
      fields = List()
    )
  }

  def generateUserFields = {
    SystemFields.generateAll
  }

  def generateFileModel = {
    Model(
      id = Cuid.createCuid(),
      name = "File",
      isSystem = true,
      fields = List()
    )
  }

  def generateFileFields = {
    SystemFields.generateAll ++
      List(
        Field(
          id = Cuid.createCuid(),
          name = "secret",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = true,
          isSystem = true,
          isReadonly = true
        ),
        Field(
          id = Cuid.createCuid(),
          name = "url",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = true,
          isSystem = true,
          isReadonly = true
        ),
        Field(
          id = Cuid.createCuid(),
          name = "name",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = false,
          isSystem = true,
          isReadonly = false
        ),
        Field(
          id = Cuid.createCuid(),
          name = "contentType",
          typeIdentifier = TypeIdentifier.String,
          isRequired = true,
          isList = false,
          isUnique = false,
          isSystem = true,
          isReadonly = true
        ),
        Field(
          id = Cuid.createCuid(),
          name = "size",
          typeIdentifier = TypeIdentifier.Int,
          isRequired = true,
          isList = false,
          isUnique = false,
          isSystem = true,
          isReadonly = true
        )
      )
  }

  def generateExampleProject(projectDatabase: ProjectDatabase) = {
    Project(
      id = Cuid.createCuid(),
      ownerId = "just-a-temporary-dummy-gets-set-to-real-client-id-later",
      name = "Example Project",
      models = List.empty,
      projectDatabase = projectDatabase
    )
  }

  def createInternalStructureForNewProject(client: Client,
                                           project: Project,
                                           projectQueries: ProjectQueries,
                                           internalDatabase: DatabaseDef,
                                           ignoreDuplicateNameVerificationError: Boolean = false)(implicit inj: Injector) = {
    List(
      CreateProject(
        client = client,
        project = project,
        projectQueries = projectQueries,
        internalDatabase = internalDatabase,
        ignoreDuplicateNameVerificationError = ignoreDuplicateNameVerificationError
      ),
      CreateSeat(
        client,
        project,
        Seat(id = Cuid.createCuid(), status = SeatStatus.JOINED, isOwner = true, email = client.email, clientId = Some(client.id), name = None),
        internalDatabase,
        ignoreDuplicateNameVerificationError = true
      )
    ) ++
      project.models.map(model => CreateModel(project = project, model = model)) ++
      project.relations.map(relation => CreateRelation(project = project.copy(relations = List()), relation = relation, clientDbQueries = EmptyClientDbQueries)) ++
      project.models.flatMap(
        model =>
          models.ModelPermission.publicPermissions
            .map(CreateModelPermission(project, model, _))) ++ project.relations.flatMap(relation =>
      models.RelationPermission.publicPermissions.map(CreateRelationPermission(project, relation, _)))

  }

  def createClientDatabaseStructureForNewProject(client: Client, project: Project, internalDatabase: DatabaseDef) = {
    List(CreateClientDatabaseForProject(projectId = project.id)) ++
      project.models.map(model => CreateModelTable(projectId = project.id, model = model)) ++
      project.models.flatMap(model => {
        model.fields
          .filter(f => !DatabaseMutationBuilder.implicitlyCreatedColumns.contains(f.name))
          .filter(_.isScalar)
          .map(field => CreateColumn(projectId = project.id, model = model, field = field))
      }) ++
      project.relations.map(relation => CreateRelationTable(project = project, relation = relation))

  }

  def createIntegrationsForNewProject(project: Project)(implicit inj: Injector) = {
    val searchProviderAlgolia = models.SearchProviderAlgolia(
      id = Cuid.createCuid(),
      subTableId = Cuid.createCuid(),
      applicationId = "",
      apiKey = "",
      algoliaSyncQueries = List(),
      isEnabled = false,
      name = IntegrationName.SearchProviderAlgolia
    )

    List(
      CreateAuthProvider(project = project, name = IntegrationName.AuthProviderEmail, metaInformation = None, isEnabled = false),
      CreateAuthProvider(project = project, name = IntegrationName.AuthProviderAuth0, metaInformation = None, isEnabled = false),
      CreateAuthProvider(project = project, name = IntegrationName.AuthProviderDigits, metaInformation = None, isEnabled = false),
      CreateIntegration(project, searchProviderAlgolia),
      CreateSearchProviderAlgolia(project, searchProviderAlgolia)
    )
  }

  def generateActions(
      name: String,
      auth0Id: String,
      email: String,
      source: CustomerSource,
      internalDatabase: DatabaseDef,
      projectQueries: ProjectQueries,
      projectDatabase: ProjectDatabase
  )(implicit inj: Injector, dispatcher: ExecutionContextExecutor, config: Config): (List[Mutaction], Client, Project) = {

    var actions: List[Mutaction] = List()

    val userFields = AuthenticateCustomerMutation.generateUserFields
    val userModel  = AuthenticateCustomerMutation.generateUserModel.copy(fields = userFields)

    val fileFields = AuthenticateCustomerMutation.generateFileFields
    val fileModel  = AuthenticateCustomerMutation.generateFileModel.copy(fields = fileFields)

    val exampleProject = generateExampleProject(projectDatabase).copy(models = List(userModel, fileModel))

    val client = models.Client(
      id = Cuid.createCuid(),
      name = name,
      auth0Id = Some(auth0Id),
      isAuth0IdentityProviderEmail = auth0Id.split("\\|").head == "auth0",
      email = email,
      hashedPassword = Cuid.createCuid(),
      resetPasswordSecret = Some(Cuid.createCuid()),
      source = source,
      projects = List(),
      createdAt = org.joda.time.DateTime.now,
      updatedAt = org.joda.time.DateTime.now
    )

    val newProject = exampleProject.copy(ownerId = client.id, models = List(userModel.copy(fields = userFields), fileModel.copy(fields = fileFields)))
    val newClient  = client.copy(projects = List(newProject))

    actions :+= CreateClient(client = client)
    actions :+= JoinPendingSeats(client)
    actions :+= InvalidateSchemaForAllProjects(client)
    actions ++= createInternalStructureForNewProject(client, newProject, projectQueries, internalDatabase)
    actions ++= createClientDatabaseStructureForNewProject(client, newProject, internalDatabase)
    actions ++= createIntegrationsForNewProject(newProject)

    (actions, newClient, newProject)
  }
}
