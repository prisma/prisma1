package cool.graph.system.schema.types

import cool.graph.shared.{ApiMatrixFactory, models}
import cool.graph.system.schema.types.ModelPermission.ModelPermissionContext
import cool.graph.system.schema.types._Field.FieldContext
import cool.graph.system.{RequestPipelineSchemaResolver, SystemUserContext}
import org.atteo.evo.inflector.English.plural
import sangria.relay._
import sangria.schema._
import scaldi.Injectable

object Model extends Injectable {

  val operationTypeArgument =
    Argument("operation", Operation.Type)

  val requestPipelineOperationTypeArgument =
    Argument("operation", RequestPipelineMutationOperation.Type)

  val bindingArgument =
    Argument("binding", FunctionBinding.Type)

  case class ModelContext(project: models.Project, model: models.Model) extends Node {
    def id = model.id
  }
  lazy val Type: ObjectType[SystemUserContext, ModelContext] = {
    val relatedModelNameArg = Argument("relatedModelName", StringType)
    ObjectType(
      "Model",
      "This is a model",
      interfaces[SystemUserContext, ModelContext](nodeInterface),
      idField[SystemUserContext, ModelContext] ::
        fields[SystemUserContext, ModelContext](
        Field("name", StringType, resolve = _.value.model.name),
        Field("namePlural", StringType, resolve = ctx => plural(ctx.value.model.name)),
        Field("description", OptionType(StringType), resolve = _.value.model.description),
        Field("isSystem", BooleanType, resolve = _.value.model.isSystem),
        Field(
          "fields",
          fieldConnection,
          arguments = Connection.Args.All,
          resolve = ctx => {
            implicit val inj = ctx.ctx.injector
            val apiMatrix    = inject[ApiMatrixFactory].create(ctx.value.project)
            val fields =
              apiMatrix
                .filterFields(ctx.value.model.fields)
                .sortBy(_.id)
                .map(field => FieldContext(ctx.value.project, field))

            Connection
              .connectionFromSeq(fields, ConnectionArgs(ctx))
          }
        ),
        Field(
          "permissions",
          modelPermissionConnection,
          arguments = Connection.Args.All,
          resolve = ctx => {
            val permissions = ctx.value.model.permissions
              .sortBy(_.id)
              .map(modelPermission => ModelPermissionContext(ctx.value.project, modelPermission))

            Connection.connectionFromSeq(permissions, ConnectionArgs(ctx))
          }
        ),
        Field("itemCount",
              IntType,
              resolve = ctx =>
                ctx.ctx
                  .dataResolver(project = ctx.value.project)
                  .itemCountForModel(ctx.value.model)),
        Field(
          "permissionSchema",
          StringType,
          arguments = List(operationTypeArgument),
          resolve = ctx => {
            ctx.ctx.getModelPermissionSchema(ctx.value.project, ctx.value.id, ctx.arg(operationTypeArgument))
          }
        ),
        Field(
          "requestPipelineFunctionSchema",
          StringType,
          arguments = List(requestPipelineOperationTypeArgument, bindingArgument),
          resolve = ctx => {

            val schemaResolver = new RequestPipelineSchemaResolver()
            val schema         = schemaResolver.resolve(ctx.value.project, ctx.value.model, ctx.arg(bindingArgument), ctx.arg(requestPipelineOperationTypeArgument))

            schema
          }
        ),
        Field(
          "permissionQueryArguments",
          ListType(PermissionQueryArgument.Type),
          arguments = List(operationTypeArgument),
          resolve = ctx => {
            ctx.arg(operationTypeArgument) match {
              case models.ModelOperation.Read =>
                PermissionQueryArguments.getReadArguments(ctx.value.model)
              case models.ModelOperation.Create =>
                PermissionQueryArguments.getCreateArguments(ctx.value.model)
              case models.ModelOperation.Update =>
                PermissionQueryArguments.getUpdateArguments(ctx.value.model)
              case models.ModelOperation.Delete =>
                PermissionQueryArguments.getDeleteArguments(ctx.value.model)
            }
          }
        )
      )
    )
  }
}
