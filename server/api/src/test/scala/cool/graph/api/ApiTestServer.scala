package cool.graph.api

import cool.graph.api.database.DataResolver
import cool.graph.api.database.deferreds.DeferredResolverProvider
import cool.graph.api.schema.{ApiUserContext, SchemaBuilder}
import cool.graph.shared.models.{AuthenticatedRequest, AuthenticatedUser, Project}
import cool.graph.util.json.SprayJsonExtensions
import cool.graph.api.server.JsonMarshalling._
//import cool.graph.util.ErrorHandlerFactory
import org.scalatest.{BeforeAndAfterEach, Suite}
import sangria.execution.{ErrorWithResolver, Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer
import spray.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.reflect.io.File

case class ApiTestServer()(implicit dependencies: ApiDependencies) extends SprayJsonExtensions with GraphQLResponseAssertions {

//  private lazy val errorHandlerFactory = ErrorHandlerFactory(println, injector.cloudwatch, injector.bugsnagger)

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  def printSchema: Boolean = false
  def writeSchemaToFile    = false
  def logSimple: Boolean   = false

//  def requestContext =
//    RequestContext(
//      CombinedTestDatabase.testClientId,
//      requestId = CombinedTestDatabase.requestId,
//      requestIp = CombinedTestDatabase.requestIp,
//      println(_),
//      projectId = Some(CombinedTestDatabase.testProjectId)
//    )

  /**
    * Execute a Query that must succeed.
    */
  def querySimple(query: String)(implicit project: Project): JsValue                       = executeQuerySimple(query, project)
  def querySimple(query: String, dataContains: String)(implicit project: Project): JsValue = executeQuerySimple(query, project, dataContains)

  def executeQuerySimple(
      query: String,
      project: Project,
      dataContains: String = "",
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): JsValue = {
    val result = executeQuerySimpleWithAuthentication(
      query = query,
      project = project,
      variables = variables,
      requestId = requestId
    )

    result.assertSuccessfulResponse(dataContains)
    result
  }

  /**
    * Execute a Query that must fail.
    */
  def querySimpleThatMustFail(query: String, errorCode: Int)(implicit project: Project): JsValue = executeQuerySimpleThatMustFail(query, project, errorCode)
  def querySimpleThatMustFail(query: String, errorCode: Int, errorCount: Int)(implicit project: Project): JsValue =
    executeQuerySimpleThatMustFail(query = query, project = project, errorCode = errorCode, errorCount = errorCount)
  def querySimpleThatMustFail(query: String, errorCode: Int, errorContains: String)(implicit project: Project): JsValue =
    executeQuerySimpleThatMustFail(query = query, project = project, errorCode = errorCode, errorContains = errorContains)
  def querySimpleThatMustFail(query: String, errorCode: Int, errorContains: String, errorCount: Int)(implicit project: Project): JsValue =
    executeQuerySimpleThatMustFail(query = query, project = project, errorCode = errorCode, errorCount = errorCount, errorContains = errorContains)

  def executeQuerySimpleThatMustFail(query: String, project: Project, userId: String, errorCode: Int): JsValue =
    executeQuerySimpleThatMustFail(query = query, project = project, userId = Some(userId), errorCode = errorCode)
  def executeQuerySimpleThatMustFail(query: String, project: Project, userId: String, errorCode: Int, errorCount: Int): JsValue =
    executeQuerySimpleThatMustFail(query = query, project = project, userId = Some(userId), errorCode = errorCode, errorCount = errorCount)
  def executeQuerySimpleThatMustFail(query: String, project: Project, errorCode: Int, errorContains: String, userId: String): JsValue =
    executeQuerySimpleThatMustFail(query = query, project = project, userId = Some(userId), errorCode = errorCode, errorContains = errorContains)
  def executeQuerySimpleThatMustFail(query: String, project: Project, userId: String, errorCode: Int, errorCount: Int, errorContains: String): JsValue =
    executeQuerySimpleThatMustFail(query = query,
                                   project = project,
                                   userId = Some(userId),
                                   errorCode = errorCode,
                                   errorCount = errorCount,
                                   errorContains = errorContains)

  def executeQuerySimpleThatMustFail(query: String,
                                     project: Project,
                                     errorCode: Int,
                                     errorCount: Int = 1,
                                     errorContains: String = "",
                                     userId: Option[String] = None,
                                     variables: JsValue = JsObject(),
                                     requestId: String = "CombinedTestDatabase.requestId",
                                     graphcoolHeader: Option[String] = None): JsValue = {
    val result = executeQuerySimpleWithAuthentication(
      query = query,
      project = project,
      authenticatedRequest = userId.map(AuthenticatedUser(_, "User", "test-token")),
      variables = variables,
      requestId = requestId,
      graphcoolHeader = graphcoolHeader
    )
    result.assertFailingResponse(errorCode, errorCount, errorContains)
    result
  }

  /**
    * Execute a Query without Checks.
    */
  def executeQuerySimpleWithAuthentication(query: String,
                                           project: Project,
                                           authenticatedRequest: Option[AuthenticatedRequest] = None,
                                           variables: JsValue = JsObject(),
                                           requestId: String = "CombinedTestDatabase.requestId",
                                           graphcoolHeader: Option[String] = None): JsValue = {

//    val unhandledErrorLogger = errorHandlerFactory.unhandledErrorHandler(
//      requestId = requestId,
//      query = query,
//      projectId = Some(project.id)
//    )
//
//    val sangriaErrorHandler = errorHandlerFactory.sangriaHandler(
//      requestId = requestId,
//      query = query,
//      variables = JsObject.empty,
//      clientId = None,
//      projectId = Some(project.id)
//    )

//    val projectLockdownMiddleware = ProjectLockdownMiddleware(project)
    val schemaBuilder  = SchemaBuilder()(dependencies.system, dependencies)
    val userContext    = ApiUserContext(clientId = "clientId")
    val schema         = schemaBuilder(project)
    val renderedSchema = SchemaRenderer.renderSchema(schema)

    if (printSchema) println(renderedSchema)
    if (writeSchemaToFile) writeSchemaIntoFile(renderedSchema)

    val queryAst = QueryParser.parse(query).get

    val context = userContext
//      UserContext
//      .fetchUser(
//        authenticatedRequest = authenticatedRequest,
//        requestId = requestId,
//        requestIp = CombinedTestDatabase.requestIp,
//        clientId = CombinedTestDatabase.testClientId,
//        project = project,
//        log = x => if (logSimple) println(x),
//        queryAst = Some(queryAst)
//      )
//    context.addFeatureMetric(FeatureMetric.ApiSimple)
//    context.graphcoolHeader = graphcoolHeader

    val result = Await.result(
      Executor
        .execute(
          schema = schema,
          queryAst = queryAst,
          userContext = context,
          variables = variables,
//          exceptionHandler = sangriaErrorHandler,
          deferredResolver = new DeferredResolverProvider(dataResolver = DataResolver(project))
//          middleware = List(apiMetricMiddleware, projectLockdownMiddleware)
        )
        .recover {
          case error: QueryAnalysisError => error.resolveError
          case error: ErrorWithResolver  =>
//            unhandledErrorLogger(error)
            error.resolveError
//          case error: Throwable ⇒ unhandledErrorLogger(error)._2

        },
      Duration.Inf
    )
    println("Request Result: " + result)
    result
  }
}
