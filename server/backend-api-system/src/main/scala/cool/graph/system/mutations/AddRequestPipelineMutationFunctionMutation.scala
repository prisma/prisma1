package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.errors.UserInputErrors
import cool.graph.shared.models
import cool.graph.shared.models.FunctionBinding.FunctionBinding
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.RequestPipelineOperation.RequestPipelineOperation
import cool.graph.shared.models._
import cool.graph.shared.mutactions.InvalidInput
import cool.graph.system.mutactions.internal.validations.URLValidation
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateFunction, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class AddRequestPipelineMutationFunctionMutation(client: models.Client,
                                                      project: models.Project,
                                                      args: AddRequestPipelineMutationFunctionInput,
                                                      projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[AddRequestPipelineMutationFunctionMutationPayload] {

  val newDelivery: FunctionDelivery = args.functionType match {
    case FunctionType.WEBHOOK =>
      WebhookFunction(url = URLValidation.getAndValidateURL(args.name, args.webhookUrl), headers = HttpFunctionHeaders.read(args.headers))

    case FunctionType.CODE if args.inlineCode.nonEmpty =>
      Auth0Function(
        code = args.inlineCode.get,
        codeFilePath = args.codeFilePath,
        url = URLValidation.getAndValidateURL(args.name, args.webhookUrl),
        auth0Id = args.auth0Id.get,
        headers = HttpFunctionHeaders.read(args.headers)
      )

    case FunctionType.CODE if args.inlineCode.isEmpty =>
      ManagedFunction(args.codeFilePath)
  }

  val newFunction = RequestPipelineFunction(
    id = args.id,
    name = args.name,
    isActive = args.isActive,
    binding = args.binding,
    modelId = args.modelId,
    operation = args.operation,
    delivery = newDelivery
  )

  val updatedProject: Project = project.copy(functions = project.functions :+ newFunction)

  override def prepareActions(): List[Mutaction] = {

    projectAlreadyHasSameRequestPipeLineFunction match {
      case true =>
        actions = List(
          InvalidInput(
            UserInputErrors
              .SameRequestPipeLineFunctionAlreadyExists(modelName = project.getModelById_!(args.modelId).name,
                                                        operation = args.operation.toString,
                                                        binding = args.binding.toString)))
      case false =>
        actions = List(CreateFunction(project, newFunction), BumpProjectRevision(project = project), InvalidateSchema(project))
    }
    actions
  }

  private def projectAlreadyHasSameRequestPipeLineFunction: Boolean = {
    def isSameRequestPipeLineFunction(function: RequestPipelineFunction) = {
      function.modelId == args.modelId &&
      function.binding == args.binding &&
      function.operation == args.operation
    }
    project.functions.collect { case function: RequestPipelineFunction if isSameRequestPipeLineFunction(function) => function }.nonEmpty
  }

  override def getReturnValue(): Option[AddRequestPipelineMutationFunctionMutationPayload] = {
    Some(AddRequestPipelineMutationFunctionMutationPayload(args.clientMutationId, project, newFunction))
  }
}

case class AddRequestPipelineMutationFunctionMutationPayload(clientMutationId: Option[String],
                                                             project: models.Project,
                                                             function: models.RequestPipelineFunction)
    extends Mutation

case class AddRequestPipelineMutationFunctionInput(clientMutationId: Option[String],
                                                   projectId: String,
                                                   name: String,
                                                   binding: FunctionBinding,
                                                   modelId: String,
                                                   isActive: Boolean,
                                                   operation: RequestPipelineOperation,
                                                   functionType: FunctionType,
                                                   webhookUrl: Option[String],
                                                   headers: Option[String],
                                                   inlineCode: Option[String],
                                                   auth0Id: Option[String],
                                                   codeFilePath: Option[String] = None) {
  val id: String = Cuid.createCuid()
}
