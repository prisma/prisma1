package cool.graph.system.mutations

import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.SystemErrors.InvalidPatForProject
import cool.graph.shared.models.Project
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.authorization.SystemAuth2
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.{Injectable, Injector}

case class GenerateUserToken(project: Project, args: GenerateUserTokenInput, projectDbsFn: Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[GenerateUserTokenPayload]
    with Injectable {

  val auth                  = SystemAuth2()
  var token: Option[String] = None

  override def prepareActions(): List[Mutaction] = {

    // This is unconventional. Most system mutations rely on the caller being authenticated in system api
    // This mutation is freely available but requires you to include a valid pat for the project
    if (!isActiveRootToken && !isValidTemporaryRootToken && !isValidPlatformToken) {
      actions :+= InvalidInput(InvalidPatForProject(project.id))
    } else {
      token = Some(auth.generateNodeToken(project, args.userId, args.modelName, args.expirationInSeconds))
    }

    actions
  }

  private def isActiveRootToken         = project.rootTokens.exists(_.token == args.pat)
  private def isValidTemporaryRootToken = auth.isValidTemporaryRootToken(project, args.pat)
  private def isValidPlatformToken = {
    auth.clientId(args.pat) match {
      case Some(clientId) => project.seats.exists(_.clientId == Some(clientId))
      case None           => false
    }
  }

  override def getReturnValue: Option[GenerateUserTokenPayload] = {
    token.map(token => GenerateUserTokenPayload(clientMutationId = args.clientMutationId, token = token))
  }
}

case class GenerateUserTokenPayload(clientMutationId: Option[String], token: String) extends Mutation

case class GenerateUserTokenInput(clientMutationId: Option[String],
                                  pat: String,
                                  projectId: String,
                                  userId: String,
                                  modelName: String,
                                  expirationInSeconds: Option[Int])
