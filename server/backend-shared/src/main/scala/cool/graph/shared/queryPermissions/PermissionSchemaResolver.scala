package cool.graph.shared.queryPermissions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.LazyLogging
import cool.graph.client.UserContext
import cool.graph.client.database.DeferredTypes.ManyModelExistsDeferred
import cool.graph.client.schema.simple.SimpleSchemaModelObjectTypeBuilder
import cool.graph.shared.{ApiMatrixFactory, models}
import cool.graph.shared.models.Project
import sangria.execution.Executor
import sangria.introspection.introspectionQuery
import sangria.schema.{Context, Field, ObjectType, Schema}
import scaldi.{Injectable, Injector}
import spray.json.JsObject

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class PermissionSchemaResolver(implicit inj: Injector) extends Injectable with LazyLogging {

  import sangria.marshalling.sprayJson._

  def resolve(project: Project): Future[String] = {

    implicit val system       = inject[ActorSystem](identified by "actorSystem")
    implicit val materializer = inject[ActorMaterializer](identified by "actorMaterializer")

    val permissionSchema = PermissionSchemaResolver.permissionSchema(project)

    Executor
      .execute(
        schema = permissionSchema,
        queryAst = introspectionQuery,
        userContext = new UserContext(
          project = project,
          authenticatedRequest = None,
          requestId = "PermissionSchemaResolver-request-id",
          requestIp = "PermissionSchemaResolver-request-ip",
          clientId = "PermissionSchemaResolver-client-id",
          log = (_) => (),
          queryAst = Some(introspectionQuery)
        )
      )
      .map { response =>
        val JsObject(fields) = response
        fields("data").compactPrint
      }
  }
}

object PermissionSchemaResolver extends Injectable {
  def permissionSchema(project: Project)(implicit inj: Injector): Schema[UserContext, Unit] = {
    val apiMatrix      = inject[ApiMatrixFactory].create(project)
    val includedModels = project.models.filter(model => apiMatrix.includeModel(model.name))
    val schemaBuilder  = new SimpleSchemaModelObjectTypeBuilder(project, None)

    def getConnectionArguments(model: models.Model) = {
      schemaBuilder.mapToListConnectionArguments(model)
    }

    def resolveGetAllItemsQuery(model: models.Model, ctx: Context[UserContext, Unit]): sangria.schema.Action[UserContext, Boolean] = {
      val arguments = schemaBuilder.extractQueryArgumentsFromContext(model, ctx)

      ManyModelExistsDeferred(model, arguments)
    }

    def getModelField(model: models.Model): Field[UserContext, Unit] = {
      Field(
        s"Some${model.name.capitalize}Exists",
        fieldType = sangria.schema.BooleanType,
        arguments = getConnectionArguments(model),
        resolve = (ctx) => {
          resolveGetAllItemsQuery(model, ctx)
        }
      )
    }

    val query    = ObjectType("Query", includedModels.map(getModelField))
    val mutation = None

    Schema(query, mutation)
  }
}
