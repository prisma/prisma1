package cool.graph.system.mutations

import com.typesafe.config.Config
import cool.graph.cuid.Cuid
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models
import cool.graph.shared.models.{Project, RootToken}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.authorization.SystemAuth2
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateRootToken, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import org.joda.time.DateTime
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class CreateRootTokenMutation(client: models.Client,
                                   project: models.Project,
                                   args: CreateRootTokenInput,
                                   projectDbsFn: models.Project => InternalAndProjectDbs)(implicit val inj: Injector)
    extends InternalProjectMutation[CreateRootTokenMutationPayload]
    with Injectable {

  val config: Config          = inject[Config](identified by "config")
  val newRootToken: RootToken = CreateRootTokenMutation.generate(clientId = client.id, projectId = project.id, name = args.name, expirationInSeconds = None)
  val updatedProject: Project = project.copy(rootTokens = project.rootTokens :+ newRootToken)

  override def prepareActions(): List[Mutaction] = {
    project.rootTokens.map(_.name).contains(newRootToken.name) match {
      case true  => actions = List(InvalidInput(UserInputErrors.RootTokenNameAlreadyInUse(newRootToken.name)))
      case false => actions = List(CreateRootToken(project.id, newRootToken), BumpProjectRevision(project), InvalidateSchema(project))
    }

    actions
  }

  override def getReturnValue: Option[CreateRootTokenMutationPayload] = {
    Some(
      CreateRootTokenMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        rootToken = newRootToken
      ))
  }
}

case class CreateRootTokenMutationPayload(clientMutationId: Option[String], project: models.Project, rootToken: models.RootToken) extends Mutation
case class CreateRootTokenInput(clientMutationId: Option[String], projectId: String, name: String, description: Option[String])

object CreateRootTokenMutation {
  private def generateRootToken(id: String, clientId: String, projectId: String, expirationInSeconds: Option[Long])(implicit inj: Injector): String = {
    SystemAuth2().generateRootToken(clientId, projectId, id, expirationInSeconds)
  }

  def generate(clientId: String, projectId: String, name: String, expirationInSeconds: Option[Long])(implicit inj: Injector): RootToken = {
    val id = Cuid.createCuid()

    models.RootToken(id = id, token = generateRootToken(id, clientId, projectId, expirationInSeconds), name = name, created = DateTime.now())
  }
}
