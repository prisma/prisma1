package cool.graph.system.mutations

import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateFunction}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateRequestPipelineMutationFunctionMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateRequestPipelineMutationFunctionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateRequestPipelineMutationFunctionMutationPayload] {

  val function: RequestPipelineFunction = project.getRequestPipelineFunction_!(args.functionId)

  val headers: Option[Seq[(String, String)]] = HttpFunctionHeaders.readOpt(args.headers)

  val updatedDelivery: FunctionDelivery =
    function.delivery.update(headers, args.functionType, args.webhookUrl.map(_.trim), args.inlineCode, args.auth0Id, args.codeFilePath)

  val updatedFunction: RequestPipelineFunction = function.copy(
    name = args.name.getOrElse(function.name),
    isActive = args.isActive.getOrElse(function.isActive),
    binding = args.binding.getOrElse(function.binding),
    modelId = args.modelId.getOrElse(function.modelId),
    operation = args.operation.getOrElse(function.operation),
    delivery = updatedDelivery
  )

  val updatedProject = project.copy(functions = project.functions.filter(_.id != function.id) :+ updatedFunction)

  override def prepareActions(): List[Mutaction] = {

    this.actions =
      List(UpdateFunction(project, newFunction = updatedFunction, oldFunction = function), BumpProjectRevision(project = project), InvalidateSchema(project))
    this.actions
  }

  override def getReturnValue: Option[UpdateRequestPipelineMutationFunctionMutationPayload] = {
    Some(UpdateRequestPipelineMutationFunctionMutationPayload(args.clientMutationId, updatedProject, updatedFunction))
  }
}

case class UpdateRequestPipelineMutationFunctionMutationPayload(clientMutationId: Option[String],
                                                                project: models.Project,
                                                                function: models.RequestPipelineFunction)
    extends Mutation

case class UpdateRequestPipelineMutationFunctionInput(clientMutationId: Option[String],
                                                      functionId: String,
                                                      name: Option[String],
                                                      isActive: Option[Boolean],
                                                      binding: Option[FunctionBinding],
                                                      modelId: Option[String],
                                                      functionType: Option[FunctionType],
                                                      operation: Option[RequestPipelineOperation],
                                                      webhookUrl: Option[String],
                                                      headers: Option[String],
                                                      inlineCode: Option[String],
                                                      auth0Id: Option[String],
                                                      codeFilePath: Option[String] = None)
