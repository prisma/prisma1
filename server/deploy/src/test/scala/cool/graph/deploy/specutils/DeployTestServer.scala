package cool.graph.deploy.specutils

import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.schema.{SchemaBuilder, SystemUserContext}
import cool.graph.shared.models.{AuthenticatedRequest, AuthenticatedUser, Project}
import sangria.execution.Executor
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer
import spray.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.reflect.io.File

case class DeployTestServer()(implicit dependencies: DeployDependencies) extends SprayJsonExtensions with GraphQLResponseAssertions {
  import cool.graph.deploy.server.JsonMarshalling._

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  def printSchema: Boolean = false
  def writeSchemaToFile    = false
  def logSimple: Boolean   = false

  /**
    * Execute a Query that must succeed.
    */
  def querySimple(query: String): JsValue                       = executeQuerySimple(query)
  def querySimple(query: String, dataContains: String): JsValue = executeQuerySimple(query, dataContains)

  // todo remove all the "simple" naming
  def executeQuerySimple(
      query: String,
      dataContains: String = "",
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): JsValue = {
    val result = executeQuerySimpleWithAuthentication(
      query = query,
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
                                           authenticatedRequest: Option[AuthenticatedRequest] = None,
                                           variables: JsValue = JsObject(),
                                           requestId: String = "CombinedTestDatabase.requestId",
                                           graphcoolHeader: Option[String] = None): JsValue = {

    val schemaBuilder  = SchemaBuilder()(dependencies.system, dependencies)
    val userContext    = SystemUserContext()
    val schema         = schemaBuilder(userContext)
    val renderedSchema = SchemaRenderer.renderSchema(schema)

    if (printSchema) println(renderedSchema)
    if (writeSchemaToFile) writeSchemaIntoFile(renderedSchema)

    val queryAst = QueryParser.parse(query).get
    val context  = userContext
    val result = Await.result(
      Executor
        .execute(
          schema = schema,
          queryAst = queryAst,
          userContext = context,
          variables = variables
          //          exceptionHandler = sangriaErrorHandler,
          //          middleware = List(apiMetricMiddleware, projectLockdownMiddleware)
        )
//        .recover {
//          case error: QueryAnalysisError => error.resolveError
//          case error: ErrorWithResolver  =>
//            //            unhandledErrorLogger(error)
//            error.resolveError
//          //          case error: Throwable ⇒ unhandledErrorLogger(error)._2
//
//        },
      ,
      Duration.Inf
    )
    println("Request Result: " + result)
    result
  }
}
