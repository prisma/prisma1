package cool.graph.client.authorization.queryPermissions

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import cool.graph.client.UserContext
import cool.graph.client.database.{DeferredResolverProvider, SimpleManyModelDeferredResolver, SimpleToManyDeferredResolver}
import cool.graph.shared.errors.UserAPIErrors.InsufficientPermissions
import cool.graph.shared.models.{AuthenticatedRequest, Project}
import cool.graph.shared.queryPermissions.PermissionSchemaResolver
import sangria.ast._
import sangria.execution.deferred.DeferredResolver
import sangria.execution.{DeprecationTracker, Executor}
import sangria.marshalling.queryAst._
import sangria.marshalling.{InputUnmarshaller, QueryAstResultMarshaller}
import sangria.parser.QueryParser
import sangria.schema.Schema
import sangria.validation.QueryValidator
import scaldi.{Injectable, Injector}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

class QueryPermissionValidator(project: Project)(implicit inj: Injector, system: ActorSystem, materializer: ActorMaterializer) extends Injectable {

  lazy val schema: Schema[UserContext, Unit] = PermissionSchemaResolver.permissionSchema(project)

  lazy val deferredResolverProvider: DeferredResolver[Any] =
    new DeferredResolverProvider(new SimpleToManyDeferredResolver, new SimpleManyModelDeferredResolver, skipPermissionCheck = true)
      .asInstanceOf[DeferredResolver[Any]]

  lazy val executor = Executor(
    schema = schema.asInstanceOf[Schema[Any, Any]],
    queryValidator = QueryValidator.default,
    deferredResolver = deferredResolverProvider,
    exceptionHandler = PartialFunction.empty,
    deprecationTracker = DeprecationTracker.empty,
    middleware = Nil,
    maxQueryDepth = None,
    queryReducers = Nil
  )

  def validate(
      query: String,
      variables: Map[String, Any],
      authenticatedRequest: Option[AuthenticatedRequest],
      alwaysQueryMasterDatabase: Boolean
  ): Future[Boolean] = {
    val context = new UserContext(
      project = project,
      authenticatedRequest = authenticatedRequest,
      requestId = "grap-permission-query",
      requestIp = "graph-permission-query",
      project.ownerId,
      (x: String) => Unit,
      alwaysQueryMasterDatabase = alwaysQueryMasterDatabase
    )

    val dataFut: Future[QueryAstResultMarshaller#Node] =
      QueryParser.parse(query) match {
        case Success(_queryAst) =>
          executor
            .execute(queryAst = _queryAst, userContext = context, root = (), variables = InputUnmarshaller.mapVars(variables))
            .recover {
              case e: Throwable => throw InsufficientPermissions(s"Permission Query is invalid. Could not be executed. Error Message: ${e.getMessage}")
            }
        case Failure(error) =>
          throw InsufficientPermissions(s"Permission Query is invalid. Could not be parsed. Error Message: ${error.getMessage}")
      }

    dataFut.map(traverseAndCheckForLeafs)
  }

  private def traverseAndCheckForLeafs(root: AstNode): Boolean = {
    root match {
      case ObjectValue(fields, _, _)   => fields.forall(field => traverseAndCheckForLeafs(field))
      case ObjectField(_, value, _, _) => traverseAndCheckForLeafs(value)
      case x: BooleanValue             => x.value
      case _                           => sys.error(s"Received unknown type of AstNode. Could not handle: $root")
    }
  }
}
