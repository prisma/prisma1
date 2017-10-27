package cool.graph.system.mutations

import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.database.InternalAndProjectDbs
import cool.graph.shared.models
import cool.graph.shared.models.FunctionType.FunctionType
import cool.graph.shared.models.{FunctionDelivery, SchemaExtensionFunction}
import cool.graph.system.mutactions.internal.{BumpProjectRevision, InvalidateSchema, UpdateFunction}
import cool.graph.{InternalProjectMutation, Mutaction}
import sangria.relay.Mutation
import scaldi.Injector

case class UpdateSchemaExtensionFunctionMutation(
    client: models.Client,
    project: models.Project,
    args: UpdateSchemaExtensionFunctionInput,
    projectDbsFn: models.Project => InternalAndProjectDbs
)(implicit inj: Injector)
    extends InternalProjectMutation[UpdateSchemaExtensionFunctionMutationPayload] {

  val function: SchemaExtensionFunction      = project.getSchemaExtensionFunction_!(args.functionId)
  val headers: Option[Seq[(String, String)]] = HttpFunctionHeaders.readOpt(args.headers)
  val updatedDelivery: FunctionDelivery =
    function.delivery.update(headers, args.functionType, args.webhookUrl.map(_.trim), args.inlineCode, args.auth0Id, args.codeFilePath)

  val updatedFunction: SchemaExtensionFunction = SchemaExtensionFunction.createFunction(
    id = function.id,
    name = args.name.getOrElse(function.name),
    isActive = args.isActive.getOrElse(function.isActive),
    schema = args.schema.getOrElse(function.schema),
    delivery = updatedDelivery,
    schemaFilePath = args.schemaFilePath
  )

  val updatedProject = project.copy(functions = project.functions.filter(_.id != function.id) :+ updatedFunction)

  override def prepareActions(): List[Mutaction] = {
    this.actions = List(
      UpdateFunction(project, newFunction = updatedFunction, oldFunction = function),
      BumpProjectRevision(project = project),
      InvalidateSchema(project)
    )

    this.actions
  }

  override def getReturnValue: Option[UpdateSchemaExtensionFunctionMutationPayload] = {
    Some(UpdateSchemaExtensionFunctionMutationPayload(args.clientMutationId, updatedProject, updatedFunction))
  }
}

case class UpdateSchemaExtensionFunctionMutationPayload(
    clientMutationId: Option[String],
    project: models.Project,
    function: models.SchemaExtensionFunction
) extends Mutation

case class UpdateSchemaExtensionFunctionInput(
    clientMutationId: Option[String],
    functionId: String,
    isActive: Option[Boolean],
    name: Option[String],
    schema: Option[String],
    functionType: Option[FunctionType],
    webhookUrl: Option[String],
    headers: Option[String],
    inlineCode: Option[String],
    auth0Id: Option[String],
    codeFilePath: Option[String] = None,
    schemaFilePath: Option[String] = None
)
