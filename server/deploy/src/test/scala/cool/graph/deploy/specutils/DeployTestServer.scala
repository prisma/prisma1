package cool.graph.deploy.specutils

import cool.graph.deploy.DeployDependencies
import cool.graph.deploy.schema.{SchemaBuilder, SystemUserContext}
import cool.graph.deploy.server.ErrorHandler
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
  def query(query: String): JsValue                       = executeQuery(query)
  def query(query: String, dataContains: String): JsValue = executeQuery(query, dataContains)

  def executeQuery(
      query: String,
      dataContains: String = "",
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): JsValue = {
    val result = executeQueryWithAuthentication(
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
  def queryThatMustFail(query: String, errorCode: Int): JsValue = executeQueryThatMustFail(query, errorCode)
  def queryThatMustFail(query: String, errorCode: Int, errorCount: Int): JsValue =
    executeQueryThatMustFail(query = query, errorCode = errorCode, errorCount = errorCount)
  def queryThatMustFail(query: String, errorCode: Int, errorContains: String): JsValue =
    executeQueryThatMustFail(query = query, errorCode = errorCode, errorContains = errorContains)
  def queryThatMustFail(query: String, errorCode: Int, errorContains: String, errorCount: Int): JsValue =
    executeQueryThatMustFail(query = query, errorCode = errorCode, errorCount = errorCount, errorContains = errorContains)

  def executeQueryThatMustFail(query: String, userId: String, errorCode: Int): JsValue =
    executeQueryThatMustFail(query = query, userId = Some(userId), errorCode = errorCode)
  def executeQueryThatMustFail(query: String, userId: String, errorCode: Int, errorCount: Int): JsValue =
    executeQueryThatMustFail(query = query, userId = Some(userId), errorCode = errorCode, errorCount = errorCount)
  def executeQueryThatMustFail(query: String, errorCode: Int, errorContains: String, userId: String): JsValue =
    executeQueryThatMustFail(query = query, userId = Some(userId), errorCode = errorCode, errorContains = errorContains)
  def executeQueryThatMustFail(query: String, userId: String, errorCode: Int, errorCount: Int, errorContains: String): JsValue =
    executeQueryThatMustFail(query = query, userId = Some(userId), errorCode = errorCode, errorCount = errorCount, errorContains = errorContains)

  def executeQueryThatMustFail(query: String,
                               errorCode: Int,
                               errorCount: Int = 1,
                               errorContains: String = "",
                               userId: Option[String] = None,
                               variables: JsValue = JsObject(),
                               requestId: String = "CombinedTestDatabase.requestId",
                               graphcoolHeader: Option[String] = None): JsValue = {
    val result = executeQueryWithAuthentication(
      query = query,
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
  def executeQueryWithAuthentication(query: String,
                                     variables: JsValue = JsObject(),
                                     requestId: String = "CombinedTestDatabase.requestId",
                                     graphcoolHeader: Option[String] = None): JsValue = {

    val schemaBuilder  = SchemaBuilder()(dependencies.system, dependencies)
    val userContext    = SystemUserContext(None)
    val schema         = schemaBuilder(userContext)
    val renderedSchema = SchemaRenderer.renderSchema(schema)
    val errorHandler   = ErrorHandler(requestId)

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
          variables = variables,
          exceptionHandler = errorHandler.sangriaExceptionHandler
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
