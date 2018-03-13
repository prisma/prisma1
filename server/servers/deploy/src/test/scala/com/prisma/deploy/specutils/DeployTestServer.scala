package com.prisma.deploy.specutils

import akka.http.scaladsl.model.HttpRequest
import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.schema.{SchemaBuilder, SystemUserContext}
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.models.{Migration, MigrationId, Project, ProjectId}
import com.prisma.utils.await.AwaitUtils
import play.api.libs.json.{JsArray, JsString}
import sangria.execution.{Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer
import spray.json._

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.reflect.io.File

case class DeployTestServer()(implicit dependencies: DeployDependencies) extends SprayJsonExtensions with GraphQLResponseAssertions with AwaitUtils {
  import com.prisma.deploy.server.JsonMarshalling._

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)

  def printSchema: Boolean = false
  def writeSchemaToFile    = false
  def logSimple: Boolean   = false

  /**
    * Execute a Query that must succeed.
    */
  def query(query: String): JsValue = executeQuery(query)

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

  def queryThatMustFail(query: String, errorCode: Int, errorContains: String): JsValue =
    executeQueryThatMustFail(query = query, errorCode = errorCode, errorContains = errorContains)

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
    val errorHandler   = ErrorHandler(requestId, HttpRequest(), query, variables.toString(), dependencies.reporter)

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
        .recover {
          case error: QueryAnalysisError => error.resolveError
        },
      Duration.Inf
    )
    println("Request Result: " + result)
    result
  }

  def addProject(name: String, stage: String) = {
    query(s"""
                    |mutation {
                    | addProject(input: {
                    |   name: "$name",
                    |   stage: "$stage"
                    | }) {
                    |   project {
                    |     name
                    |     stage
                    |   }
                    | }
                    |}
      """.stripMargin)
  }

  def deploySchema(project: Project, schema: String): Project = {
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    deployHelper(nameAndStage.name, nameAndStage.stage, schema, Vector.empty)
    dependencies.projectPersistence.load(project.id).await.get
  }

  def deploySchemaThatMustFail(project: Project, schema: String): Project = {
    val nameAndStage = ProjectId.fromEncodedString(project.id)
    deployHelper(nameAndStage.name, nameAndStage.stage, schema, Vector.empty, true)
    dependencies.projectPersistence.load(project.id).await.get
  }

  def deploySchema(name: String, stage: String, schema: String, secrets: Vector[String] = Vector.empty): (Project, Migration) = {
    val projectId = name + "@" + stage
    val revision  = deployHelper(name, stage, schema, secrets)
    (dependencies.projectPersistence.load(projectId).await.get, dependencies.migrationPersistence.byId(MigrationId(projectId, revision.toInt)).await.get)
  }

  private def deployHelper(name: String, stage: String, schema: String, secrets: Vector[String], shouldFail: Boolean = false) = {
    val formattedSchema  = JsString(schema).toString
    val secretsFormatted = JsArray(secrets.map(JsString)).toString()

    val queryString = s"""
                         |mutation {
                         |  deploy(input:{name: "$name", stage: "$stage", types: $formattedSchema, secrets: $secretsFormatted}){
                         |    migration {
                         |      applied
                         |      revision
                         |    }
                         |    errors {
                         |      description
                         |    }
                         |  }
                         |}
      """.stripMargin

    val deployResult = query(queryString)
    val errors       = deployResult.pathAsSeq("data.deploy.errors")

    if (shouldFail) require(requirement = errors.nonEmpty, message = s"The query had to result in a failure but it returned no errors.")
    else require(requirement = errors.isEmpty, message = s"The query had to result in a success but it returned errors.")

    deployResult.pathAsLong("data.deploy.migration.revision")
  }
}
