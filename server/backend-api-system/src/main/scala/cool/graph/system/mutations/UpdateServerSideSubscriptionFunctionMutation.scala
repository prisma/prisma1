package cool.graph.system.mutations

import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors.ServerSideSubscriptionQueryIsInvalid
import cool.graph.shared.models
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.{FunctionDelivery, HttpFunction, ServerSideSubscriptionFunction}
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.subscriptions.schemas.SubscriptionQueryValidator
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateFunction}
import cool.graph.{InternalProjectMutation, Mutaction}
import org.scalactic.Bad
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateServerSideSubscriptionFunctionMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateServerSideSubscriptionFunctionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateServerSideSubscriptionFunctionMutationPayload] {

  val function: ServerSideSubscriptionFunction = project.getServerSideSubscriptionFunction_!(args.functionId)

  val headers: Option[Seq[(String, String)]] = HttpFunctionHeaders.readOpt(args.headers)
  val updatedDelivery: FunctionDelivery =
    function.delivery.update(headers, args.functionType, args.webhookUrl, args.inlineCode, args.auth0Id, args.codeFilePath)

  val updatedFunction: ServerSideSubscriptionFunction = function.copy(
    name = args.name.getOrElse(function.name),
    isActive = args.isActive.getOrElse(function.isActive),
    query = args.query.getOrElse(function.query),
    queryFilePath = args.queryFilePath,
    delivery = updatedDelivery
  )

  val updatedProject = project.copy(functions = project.functions.filter(_.id != function.id) :+ updatedFunction)

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(
      UpdateFunction(project, newFunction = updatedFunction, oldFunction = function),
      BumpProjectRevision(project = project),
      InvalidateSchema(project)
    )
    if (args.query.isDefined) {
      SubscriptionQueryValidator(project).validate(args.query.get) match {
        case Bad(errors) =>
          val userError = ServerSideSubscriptionQueryIsInvalid(errors.head.errorMessage, updatedFunction.name)
          this.actions :+= InvalidInput(userError)
        case _ => // NO OP
      }
    }

    this.actions
  }

  override def getReturnValue: Option[UpdateServerSideSubscriptionFunctionMutationPayload] = {
    Some(
      UpdateServerSideSubscriptionFunctionMutationPayload(
        clientMutationId = args.clientMutationId,
        project = updatedProject,
        function = updatedFunction
      ))
  }
}

case class UpdateServerSideSubscriptionFunctionMutationPayload(
    clientMutationId: Option[String],
    project: models.Project,
    function: models.ServerSideSubscriptionFunction
) extends Mutation

case class UpdateServerSideSubscriptionFunctionInput(
    clientMutationId: Option[String],
    functionId: String,
    name: Option[String],
    isActive: Option[Boolean],
    query: Option[String],
    functionType: Option[FunctionType],
    webhookUrl: Option[String],
    headers: Option[String],
    inlineCode: Option[String],
    auth0Id: Option[String],
    codeFilePath: Option[String] = None,
    queryFilePath: Option[String] = None
)
