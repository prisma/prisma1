package com.prisma.api

import com.prisma.api.schema.{ApiUserContext, PrivateSchemaBuilder, SchemaBuilder}
import com.prisma.shared.models.Project
import com.prisma.utils.json.PlayJsonExtensions
import play.api.libs.json._
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer
import sangria.schema.Schema

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Awaitable, Future}
import scala.reflect.io.File

case class ApiTestServer()(implicit dependencies: ApiDependencies) extends PlayJsonExtensions {
  import dependencies.system.dispatcher

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  def printSchema: Boolean = false
  def writeSchemaToFile    = false
  def logSimple: Boolean   = false

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

  private def querySchemaAsync(
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

  private def awaitInfinitely[T](awaitable: Awaitable[T]): T = Await.result(awaitable, Duration.Inf)
}
