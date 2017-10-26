package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.ServerSideSubscriptionQueryIsInvalid
import cool.graph.shared.models
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.subscriptions.schemas.SubscriptionQueryValidator
import cool.graph.system.mutactions.internal.validations.URLValidation
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateFunction, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import org.scalactic.Bad
import sangria.relay.Mutation
import scaldi.Injector

case class AddServerSideSubscriptionFunctionMutation(client: models.Client,
                                                     project: models.Project,
                                                     args: AddServerSideSubscriptionFunctionInput,
                                                     projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[AddServerSideSubscriptionFunctionMutationPayload] {

  val newDelivery: FunctionDelivery = args.functionType match {
    case FunctionType.WEBHOOK =>
      WebhookFunction(url = URLValidation.getAndValidateURL(args.name, args.url), headers = HttpFunctionHeaders.read(args.headers))

    case FunctionType.CODE if args.inlineCode.nonEmpty =>
      Auth0Function(
        code = args.inlineCode.get,
        codeFilePath = args.codeFilePath,
        url = URLValidation.getAndValidateURL(args.name, args.url),
        auth0Id = args.auth0Id.get,
        headers = HttpFunctionHeaders.read(args.headers)
      )

    case FunctionType.CODE if args.inlineCode.isEmpty =>
      ManagedFunction(args.codeFilePath)
  }

  val newFunction = ServerSideSubscriptionFunction(
    id = args.id,
    name = args.name,
    isActive = args.isActive,
    query = args.query,
    queryFilePath = args.queryFilePath,
    delivery = newDelivery
  )

  val updatedProject: Project = project.copy(functions = project.functions :+ newFunction)

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(CreateFunction(project, newFunction), BumpProjectRevision(project = project), InvalidateSchema(project))

    SubscriptionQueryValidator(project).validate(args.query) match {
      case Bad(errors) =>
        val userError = ServerSideSubscriptionQueryIsInvalid(errors.head.errorMessage, newFunction.name)
        this.actions :+= InvalidInput(userError)
      case _ => // NO OP
    }

    this.actions
  }

  override def getReturnValue: Option[AddServerSideSubscriptionFunctionMutationPayload] = {
    Some(AddServerSideSubscriptionFunctionMutationPayload(args.clientMutationId, project, newFunction))
  }
}

case class AddServerSideSubscriptionFunctionMutationPayload(
    clientMutationId: Option[String],
    project: models.Project,
    function: models.ServerSideSubscriptionFunction
) extends Mutation

case class AddServerSideSubscriptionFunctionInput(
    clientMutationId: Option[String],
    projectId: String,
    name: String,
    isActive: Boolean,
    query: String,
    functionType: FunctionType,
    url: Option[String],
    headers: Option[String],
    inlineCode: Option[String],
    auth0Id: Option[String],
    codeFilePath: Option[String] = None,
    queryFilePath: Option[String] = None
) {
  val id: String = Cuid.createCuid()
}
