package cool.graph.system.schema.types

import cool.graph.shared.models
import cool.graph.shared.schema.CustomScalarTypes
import cool.graph.system.SystemUserContext
import sangria.relay._
import sangria.schema.{ObjectType, _}
import scaldi.Injector

import scala.concurrent.ExecutionContext.Implicits.global

object Customer {
  def getType(customerId: String)(implicit inj: Injector): ObjectType[SystemUserContext, models.Client] = ObjectType(
    "Customer",
    "This is a Customer",
    interfaces[SystemUserContext, models.Client](nodeInterface),
    fields[SystemUserContext, models.Client](
      idField[SystemUserContext, models.Client],
      Field("name", StringType, resolve = _.value.name),
      Field("email", StringType, resolve = _.value.email),
      Field("source", CustomerSourceType, resolve = _.value.source),
      Field("createdAt", CustomScalarTypes.DateTimeType, resolve = _.value.createdAt),
      Field("updatedAt", CustomScalarTypes.DateTimeType, resolve = _.value.updatedAt),
      Field(
        "projects",
        projectConnection,
        resolve = ctx =>
          ctx.ctx.clientResolver.resolveProjectsForClient(ctx.ctx.getClient.id).map { projects =>
            Connection.connectionFromSeq(projects.sortBy(_.id), ConnectionArgs(ctx))
        },
        arguments = Connection.Args.All
      )
    )
  )
}
