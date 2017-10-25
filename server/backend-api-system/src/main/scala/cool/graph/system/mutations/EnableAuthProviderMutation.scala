package cool.graph.system.mutations

import cool.graph._
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.IntegrationName.IntegrationName
import cool.graph.shared.models.ManagedFields.ManagedField
import cool.graph.shared.models._
import cool.graph.system.database.client.EmptyClientDbQueries
import cool.graph.system.mutactions.client.CreateColumn
import cool.graph.system.mutactions.internal._
import sangria.relay.Mutation
import scaldi.Injector

case class EnableAuthProviderMutation(
    client: models.Client,
    project: models.Project,
    args: EnableAuthProviderInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[EnableAuthProviderPayload] {

  var integrationName: IntegrationName = project.getAuthProviderById_!(args.id).name

  override def prepareActions(): List[Mutaction] = {

    val meta: Option[AuthProviderMetaInformation] = integrationName match {
      case IntegrationName.AuthProviderDigits if args.digitsConsumerKey.isDefined =>
        Some(
          AuthProviderDigits(
            id = Cuid.createCuid(),
            consumerKey = args.digitsConsumerKey.get,
            consumerSecret = args.digitsConsumerSecret.get
          ))
      case IntegrationName.AuthProviderAuth0 if args.auth0ClientId.isDefined =>
        Some(
          models.AuthProviderAuth0(
            id = Cuid.createCuid(),
            clientId = args.auth0ClientId.get,
            clientSecret = args.auth0ClientSecret.get,
            domain = args.auth0Domain.get
          ))
      case _ => None
    }

    actions ++= EnableAuthProviderMutation.getUpdateMutactions(
      client = client,
      project = project,
      integrationName = integrationName,
      metaInformation = meta,
      isEnabled = args.isEnabled
    )

    actions
  }

  override def getReturnValue: Option[EnableAuthProviderPayload] = {
    Some(EnableAuthProviderPayload(clientMutationId = args.clientMutationId, project = project, authProvider = integrationName))
  }

}

object EnableAuthProviderMutation {
  def getUpdateMutactions(
      client: Client,
      project: Project,
      integrationName: IntegrationName.IntegrationName,
      metaInformation: Option[AuthProviderMetaInformation],
      isEnabled: Boolean
  )(implicit inj: Injector): List[Mutaction] = {

    val managedFields = ManagedFields(integrationName)

    project.getModelByName("User") match {
      case Some(user) =>
        val existingAuthProvider =
          project.authProviders.find(_.name == integrationName).get

        def createManagedFields: List[Mutaction] = {
          managedFields.flatMap(createFieldMutactions(_, userModel = user, client, project))
        }

        val newMeta = metaInformation match {
          case Some(y) => metaInformation
          case None    => existingAuthProvider.metaInformation
        }

        val updateAuthProvider = UpdateAuthProvider(
          project = project,
          authProvider = existingAuthProvider.copy(isEnabled = isEnabled),
          metaInformation = newMeta,
          oldMetaInformationId = existingAuthProvider.metaInformation.map(_.id)
        )

        val fieldActions = (existingAuthProvider.isEnabled, isEnabled) match {
          case (true, false) => getMakeFieldsUnmanagedMutactions(project, managedFields)
          case (false, true) => createManagedFields
          case _             => List()
        }

        fieldActions ++ List(updateAuthProvider, BumpProjectRevision(project = project), InvalidateSchema(project))

      case None =>
        List()
    }
  }

  private def createFieldMutactions(
      managedField: ManagedField,
      userModel: Model,
      client: Client,
      project: Project
  )(implicit inj: Injector) = {
    val field = Field(
      id = Cuid.createCuid(),
      name = managedField.defaultName,
      typeIdentifier = managedField.typeIdentifier,
      description = managedField.description,
      isRequired = false,
      isList = false,
      isUnique = managedField.isUnique,
      isSystem = true,
      isReadonly = managedField.isReadonly,
      defaultValue = None,
      relation = None,
      relationSide = None
    )

    List(
      CreateColumn(projectId = project.id, model = userModel, field = field),
      CreateField(project = project, model = userModel, field = field, migrationValue = None, clientDbQueries = EmptyClientDbQueries)
    )
  }

  private def getMakeFieldsUnmanagedMutactions(
      project: Project,
      managedFields: List[ManagedField]
  )(implicit inj: Injector): List[Mutaction] = {
    // We no longer remove managed fields
    // Instead we change them to be non-managed
    project.getModelByName("User") match {
      case Some(user) =>
        managedFields.flatMap(managedField => {
          user
            .getFieldByName(managedField.defaultName)
            .map(field => {
              val updatedField = field.copy(isSystem = false, isReadonly = false)
              List(UpdateField(user, field, updatedField, None, clientDbQueries = EmptyClientDbQueries))
            })
            .getOrElse(List())

        })
      case None => List()
    }

  }
}

case class EnableAuthProviderPayload(clientMutationId: Option[String], project: models.Project, authProvider: IntegrationName) extends Mutation

case class EnableAuthProviderInput(clientMutationId: Option[String],
                                   id: String,
                                   isEnabled: Boolean,
                                   digitsConsumerKey: Option[String],
                                   digitsConsumerSecret: Option[String],
                                   auth0Domain: Option[String],
                                   auth0ClientId: Option[String],
                                   auth0ClientSecret: Option[String])
