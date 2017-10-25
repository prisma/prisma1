package cool.graph.system.mutations

import cool.graph.cuid.Cuid
import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models._
import cool.graph.system.mutactions.internal.{BumpProjectRevision, CreateFunction, InvalidateSchema}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class AddSchemaExtensionFunctionMutation(client: models.Client,
                                              project: models.Project,
                                              args: AddSchemaExtensionFunctionInput,
                                              projectDbsFn: models.Project => InternalAndProjectDbs)(implicit inj: Injector)
    extends InternalProjectMutation[AddSchemaExtensionFunctionMutationPayload] {

  val newDelivery: FunctionDelivery = args.functionType match {
    case FunctionType.WEBHOOK =>
      WebhookFunction(url = args.url.get.trim, headers = HttpFunctionHeaders.read(args.headers))

    case FunctionType.CODE if args.inlineCode.nonEmpty =>
      Auth0Function(
        code = args.inlineCode.get,
        codeFilePath = args.codeFilePath,
        url = args.url.get.trim,
        auth0Id = args.auth0Id.get,
        headers = HttpFunctionHeaders.read(args.headers)
      )

    case FunctionType.CODE if args.inlineCode.isEmpty =>
      ManagedFunction(args.codeFilePath)
  }

  val newFunction: SchemaExtensionFunction = SchemaExtensionFunction.createFunction(
    id = args.id,
    name = args.name,
    isActive = args.isActive,
    schema = args.schema,
    delivery = newDelivery,
    schemaFilePath = args.schemaFilePath
  )

  val updatedProject: Project = project.copy(functions = project.functions :+ newFunction)

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(CreateFunction(project, newFunction), BumpProjectRevision(project = project), InvalidateSchema(project))
    this.actions
  }

  override def getReturnValue: Option[AddSchemaExtensionFunctionMutationPayload] = {
    Some(AddSchemaExtensionFunctionMutationPayload(args.clientMutationId, project, newFunction))
  }
}

case class AddSchemaExtensionFunctionMutationPayload(
    clientMutationId: Option[String],
    project: models.Project,
    function: models.SchemaExtensionFunction
) extends Mutation

case class AddSchemaExtensionFunctionInput(
    clientMutationId: Option[String],
    projectId: String,
    isActive: Boolean,
    name: String,
    schema: String,
    functionType: FunctionType,
    url: Option[String],
    headers: Option[String],
    inlineCode: Option[String],
    auth0Id: Option[String],
    codeFilePath: Option[String] = None,
    schemaFilePath: Option[String] = None
) {
  val id: String = Cuid.createCuid()
}
