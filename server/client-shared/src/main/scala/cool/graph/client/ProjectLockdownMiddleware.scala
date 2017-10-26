package cool.graph.client

import com.typesafe.scalalogging.LazyLogging
import cool.graph.shared.errors.CommonErrors.{MutationsNotAllowedForProject, QueriesNotAllowedForProject}
import cool.graph.RequestContextTrait
import cool.graph.shared.models.Project
import sangria.ast.{OperationDefinition, OperationType}
import sangria.execution._

case class ProjectLockdownMiddleware(project: Project) extends Middleware[RequestContextTrait] with LazyLogging {

  override type QueryVal = Unit

  override def beforeQuery(context: MiddlewareQueryContext[RequestContextTrait, _, _]): Unit = {
    val isQuery: Boolean = context.queryAst.definitions collect {
      case x: OperationDefinition if x.operationType == OperationType.Query || x.operationType == OperationType.Subscription =>
        x
    } isDefinedAt (0)

    val isMutation: Boolean = context.queryAst.definitions collect {
      case x: OperationDefinition if x.operationType == OperationType.Mutation =>
        x
    } isDefinedAt (0)

    if (isQuery && !project.allowQueries) {
      throw new QueriesNotAllowedForProject(project.id)
    }

    if (isMutation && !project.allowMutations) {
      throw new MutationsNotAllowedForProject(project.id)
    }

    ()
  }

  override def afterQuery(queryVal: Unit, context: MiddlewareQueryContext[RequestContextTrait, _, _]): Unit = ()
}
