package com.prisma.api

import com.prisma.api.schema.{ApiUserContext, PrivateSchemaBuilder, SchemaBuilder}
import com.prisma.graphql.GraphQlClient
import com.prisma.shared.models.Project
import com.prisma.utils.json.PlayJsonExtensions
import play.api.libs.json._
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer
import sangria.schema.Schema

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, Future}
import scala.reflect.io.File
import scala.sys.process.{Process, ProcessLogger}

trait ApiTestServer extends PlayJsonExtensions {
  val dependencies: ApiDependencies

  /**
    * Execute a Query that must succeed.
    */
  def query(
      query: String,
      project: Project,
      dataContains: String = "",
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): JsValue = awaitInfinitely {
    queryAsync(query, project, dataContains, variables, requestId)
  }

  def queryAsync(
      query: String,
      project: Project,
      dataContains: String = "",
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): Future[JsValue]

  /**
    * Execute a Query that must fail.
    */
  def queryThatMustFail(
      query: String,
      project: Project,
      errorCode: Int,
      errorCount: Int = 1,
      errorContains: String = "",
      userId: Option[String] = None,
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): JsValue

  protected def querySchemaAsync(
      query: String,
      project: Project,
      schema: Schema[ApiUserContext, Unit],
      variables: JsValue,
      requestId: String
  ): Future[JsValue]

  def queryPrivateSchema(query: String, project: Project, variables: JsObject = JsObject.empty): JsValue = {
    val schemaBuilder = PrivateSchemaBuilder(project)(dependencies)
    awaitInfinitely {
      querySchemaAsync(
        query = query,
        project = project,
        schema = schemaBuilder.build(),
        variables = variables,
        requestId = "private-api-request"
      )
    }
  }

  protected def awaitInfinitely[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)
}

case class PrismaLogger() extends ProcessLogger {
  private val stdout = new StringBuffer()
  private val stderr = new StringBuffer()

  override def out(s: => String): Unit = stdout.append(s)
  override def err(s: => String): Unit = stderr.append(s)

  override def buffer[T](f: => T): T = ???

  def getStdout: String = stdout.toString
  def getStderr: String = stderr.toString
}

case class ProcessHandle(process: Process, logger: PrismaLogger) {
  def kill = process.destroy()
}

/*
 * Wip
 * - Underlying forwarder for mutations
 */
case class ExternalApiTestServer()(implicit val dependencies: ApiDependencies) extends ApiTestServer {
  import dependencies.system.dispatcher

  import sys.process._

  implicit val system       = dependencies.system
  implicit val materializer = dependencies.materializer

  val prismaBinaryPath: String       = sys.env.getOrElse("PRISMA_BINARY_PATH", sys.error("Required PRISMA_BINARY_PATH env var not found"))
  val prismaBinaryConfigPath: String = sys.env.getOrElse("PRISMA_BINARY_CONFIG_PATH", sys.error("Required PRISMA_BINARY_CONFIG_PATH env var not found"))
  val gqlClient                      = GraphQlClient("http://127.0.0.1:8000") // todo rust code currently ignores port in config

  def startPrismaProcess(): ProcessHandle = {
    val logger     = PrismaLogger()
    val workingDir = new java.io.File(".")
    val process    = Process(prismaBinaryPath, workingDir, "PRISMA_CONFIG_PATH" -> prismaBinaryConfigPath).run(logger)

    ProcessHandle(process, logger)
  }

  override def queryAsync(query: String, project: Project, dataContains: String, variables: JsValue, requestId: String): Future[JsValue] = {
    val schemaBuilder = SchemaBuilder()(dependencies)
    val result = querySchemaAsync(
      query = query.stripMargin,
      project = project,
      schema = schemaBuilder(project),
      variables = variables,
      requestId = requestId
    )

    result.map { r =>
      r.assertSuccessfulResponse(dataContains)
      r
    }
  }

  override protected def querySchemaAsync(query: String,
                                          project: Project,
                                          schema: Schema[ApiUserContext, Unit],
                                          variables: JsValue,
                                          requestId: String): Future[JsValue] = {
    // Decide whether to go through the external server or internal resolver
    if (query.startsWith("mutation")) {
      val queryAst = QueryParser.parse(query.stripMargin).get
      val result = dependencies.queryExecutor.execute(
        requestId = requestId,
        queryString = query,
        queryAst = queryAst,
        variables = variables,
        operationName = None,
        project = project,
        schema = schema
      )

      result.foreach(x => println(s"""Request Result:
          |$x
      """.stripMargin))
      result
    } else {
      val prismaProcess = startPrismaProcess()
      val res           = gqlClient.sendQuery(query).map(r => r.jsonBody.get) // todo don't unwrap

      res.onComplete(_ => {
        println(prismaProcess.logger.getStdout)
        println(prismaProcess.logger.getStderr)
        prismaProcess.kill
      })
      res
    }
  }

  override def queryThatMustFail(query: String,
                                 project: Project,
                                 errorCode: Int,
                                 errorCount: Int,
                                 errorContains: String,
                                 userId: Option[String],
                                 variables: JsValue,
                                 requestId: String): JsValue = {
    val schemaBuilder = SchemaBuilder()(dependencies)
    val result = awaitInfinitely {
      querySchemaAsync(
        query = query,
        project = project,
        schema = schemaBuilder(project),
        variables = variables,
        requestId = requestId
      )
    }

    result.assertFailingResponse(errorCode, errorCount, errorContains)
    result
  }
}

case class InternalApiTestServer()(implicit val dependencies: ApiDependencies) extends ApiTestServer {
  import dependencies.system.dispatcher

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  def printSchema: Boolean = false
  def writeSchemaToFile    = false
  def logSimple: Boolean   = false

  def queryAsync(
      query: String,
      project: Project,
      dataContains: String = "",
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): Future[JsValue] = {
    val schemaBuilder = SchemaBuilder()(dependencies)
    val result = querySchemaAsync(
      query = query.stripMargin,
      project = project,
      schema = schemaBuilder(project),
      variables = variables,
      requestId = requestId
    )

    result.map { r =>
      r.assertSuccessfulResponse(dataContains)
      r
    }
  }

  def queryThatMustFail(
      query: String,
      project: Project,
      errorCode: Int,
      errorCount: Int = 1,
      errorContains: String = "",
      userId: Option[String] = None,
      variables: JsValue = JsObject.empty,
      requestId: String = "CombinedTestDatabase.requestId"
  ): JsValue = {
    val schemaBuilder = SchemaBuilder()(dependencies)
    val result = awaitInfinitely {
      querySchemaAsync(
        query = query,
        project = project,
        schema = schemaBuilder(project),
        variables = variables,
        requestId = requestId
      )
    }

    result.assertFailingResponse(errorCode, errorCount, errorContains)
    result
  }

  def querySchemaAsync(
      query: String,
      project: Project,
      schema: Schema[ApiUserContext, Unit],
      variables: JsValue,
      requestId: String
  ): Future[JsValue] = {
    val queryAst = QueryParser.parse(query.stripMargin).get

    lazy val renderedSchema = SchemaRenderer.renderSchema(schema)
    if (printSchema) println(renderedSchema)
    if (writeSchemaToFile) writeSchemaIntoFile(renderedSchema)

    val result = dependencies.queryExecutor.execute(
      requestId = requestId,
      queryString = query,
      queryAst = queryAst,
      variables = variables,
      operationName = None,
      project = project,
      schema = schema
    )

    result.foreach(x => println(s"""Request Result:
        |$x
      """.stripMargin))
    result
  }

}
