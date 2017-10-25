package cool.graph.system.schema.types

import cool.graph.shared.adapters.HttpFunctionHeaders
import cool.graph.shared.models
import cool.graph.shared.models._
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.SystemUserContext
import cool.graph.system.schema.types.Function._
import cool.graph.system.schema.types.Model.ModelContext
import sangria.relay._
import sangria.schema.{Field, _}

import scala.concurrent.ExecutionContext.Implicits.global

object Function {
  trait FunctionInterface {
    val project: models.Project
    val function: models.Function
  }

  case class FunctionContextRp(project: models.Project, function: RequestPipelineFunction) extends Node with FunctionInterface {
    override def id: String = function.id
  }

  case class FunctionContextSss(project: models.Project, function: ServerSideSubscriptionFunction) extends Node with FunctionInterface {
    override def id: String = function.id
  }

  case class FunctionContextSchemaExtension(project: models.Project, function: SchemaExtensionFunction) extends Node with FunctionInterface {
    override def id: String = function.id
  }

  def mapToContext(project: Project, function: models.Function): FunctionInterface = {
    function match {
      case rp: RequestPipelineFunction                => FunctionContextRp(project, rp)
      case sss: models.ServerSideSubscriptionFunction => FunctionContextSss(project, sss)
      case cm: models.CustomMutationFunction          => FunctionContextSchemaExtension(project, cm)
      case cq: models.CustomQueryFunction             => FunctionContextSchemaExtension(project, cq)
    }
  }

  lazy val Type: InterfaceType[SystemUserContext, FunctionInterface] = InterfaceType(
    "Function",
    "This is a Function",
    fields[SystemUserContext, FunctionInterface](
      Field("id", IDType, resolve = _.value.function.id),
      Field(
        "logs",
        logConnection,
        arguments = Connection.Args.All,
        resolve = ctx => {
          ctx.ctx.logsDataResolver
            .load(ctx.value.function.id)
            .map(logs => {
              // todo: don't rely on in-mem connections generation
              Connection.connectionFromSeq(logs, ConnectionArgs(ctx))
            })
        }
      ),
      Field("stats", FunctionStats.Type, arguments = Connection.Args.All, resolve = ctx => {
        ctx.value
      }),
      Field("name", StringType, resolve = ctx => ctx.value.function.name),
      Field("type", FunctionType.Type, resolve = ctx => ctx.value.function.delivery.functionType),
      Field("isActive", BooleanType, resolve = ctx => ctx.value.function.isActive),
      Field("webhookUrl", OptionType(StringType), resolve = _.value.function.delivery match {
        case x: HttpFunction => Some(x.url)
        case _               => None
      }),
      Field(
        "webhookHeaders",
        OptionType(StringType),
        resolve = _.value.function.delivery match {
          case x: HttpFunction => Some(HttpFunctionHeaders.write(x.headers).toString)
          case _               => None
        }
      ),
      Field("inlineCode", OptionType(StringType), resolve = _.value.function.delivery match {
        case x: CodeFunction => Some(x.code)
        case _               => None
      }),
      Field("auth0Id", OptionType(StringType), resolve = _.value.function.delivery match {
        case x: Auth0Function => Some(x.auth0Id)
        case _                => None
      })
    )
  )
}

object RequestPipelineMutationFunction {
  lazy val Type: ObjectType[SystemUserContext, FunctionContextRp] =
    ObjectType[SystemUserContext, FunctionContextRp](
      "RequestPipelineMutationFunction",
      "This is a RequestPipelineMutationFunction",
      interfaces[SystemUserContext, FunctionContextRp](nodeInterface, Function.Type),
      fields[SystemUserContext, FunctionContextRp](
        Field(
          "model",
          ModelType,
          resolve = ctx => {
            val modelId = ctx.value.function.modelId
            val model   = ctx.value.project.getModelById_!(modelId)
            ModelContext(ctx.value.project, model)
          }
        ),
        Field("binding", FunctionBinding.Type, resolve = ctx => { ctx.value.function.binding }),
        Field("operation", RequestPipelineMutationOperation.Type, resolve = _.value.function.operation)
      )
    )
}

object RequestPipelineMutationOperation {
  val Type = EnumType(
    "RequestPipelineMutationOperation",
    values = List(
      EnumValue("CREATE", value = models.RequestPipelineOperation.CREATE),
      EnumValue("UPDATE", value = models.RequestPipelineOperation.UPDATE),
      EnumValue("DELETE", value = models.RequestPipelineOperation.DELETE)
    )
  )
}

object FunctionStats {
  lazy val Type: ObjectType[SystemUserContext, FunctionInterface] = ObjectType[SystemUserContext, FunctionInterface](
    "FunctionStats",
    "This is statistics for a Function",
    fields[SystemUserContext, FunctionInterface](
      Field(
        "requestHistogram",
        ListType(IntType),
        resolve = ctx => {

          ctx.ctx.logsDataResolver.calculateHistogram(
            projectId = ctx.value.project.id,
            period = cool.graph.system.database.finder.HistogramPeriod.HALF_HOUR,
            functionId = Some(ctx.value.function.id)
          )
        }
      ),
      Field("requestCount", IntType, resolve = ctx => {
        ctx.ctx.logsDataResolver.countRequests(ctx.value.function.id)
      }),
      Field("errorCount", IntType, resolve = ctx => {
        ctx.ctx.logsDataResolver.countErrors(ctx.value.function.id)
      }),
      Field(
        "lastRequest",
        OptionType(CustomScalarTypes.DateTimeType),
        resolve = ctx => {
          ctx.ctx.logsDataResolver
            .load(ctx.value.function.id, 1)
            .map(_.headOption.map(_.timestamp))
        }
      )
    )
  )
}

object ServerSideSubscriptionFunction {
  lazy val Type: ObjectType[SystemUserContext, FunctionContextSss] =
    ObjectType[SystemUserContext, FunctionContextSss](
      "ServerSideSubscriptionFunction",
      "This is a ServerSideSubscriptionFunction",
      interfaces[SystemUserContext, FunctionContextSss](nodeInterface, Function.Type),
      fields[SystemUserContext, FunctionContextSss](
        Field("query", StringType, resolve = _.value.function.query)
      )
    )
}

object SchemaExtensionFunction {
  lazy val Type: ObjectType[SystemUserContext, FunctionContextSchemaExtension] =
    ObjectType[SystemUserContext, FunctionContextSchemaExtension](
      "SchemaExtensionFunction",
      "This is a SchemaExtensionFunction",
      interfaces[SystemUserContext, FunctionContextSchemaExtension](nodeInterface, Function.Type),
      fields[SystemUserContext, FunctionContextSchemaExtension](
        Field("schema", StringType, resolve = _.value.function.schema)
      )
    )
}
