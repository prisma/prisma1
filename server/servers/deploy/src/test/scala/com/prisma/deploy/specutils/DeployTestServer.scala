package com.prisma.deploy.specutils

import com.prisma.deploy.DeployDependencies
import com.prisma.deploy.schema.{DeployApiError, SchemaBuilder, SystemUserContext}
import com.prisma.sangria.utils.ErrorHandler
import com.prisma.shared.models.{Migration, MigrationId, Project}
import com.prisma.utils.await.AwaitUtils
import com.prisma.utils.json.PlayJsonExtensions
import play.api.libs.json.{JsArray, JsString, _}
import sangria.execution.{Executor, QueryAnalysisError}
import sangria.parser.QueryParser
import sangria.renderer.SchemaRenderer

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.reflect.io.File

case class DeployTestServer()(implicit dependencies: DeployDependencies) extends PlayJsonExtensions with AwaitUtils {
  import com.prisma.deploy.server.JsonMarshalling._

  def writeSchemaIntoFile(schema: String): Unit = File("schema").writeAll(schema)
  def printSchema: Boolean                      = false
  def writeSchemaToFile: Boolean                = false
  def logSimple: Boolean                        = false

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
                               variables: JsValue = JsObject.empty,
                               requestId: String = "CombinedTestDatabase.requestId",
                               prismaHeader: Option[String] = None): JsValue = {
    val result = executeQueryWithAuthentication(
      query = query,
      variables = variables,
      requestId = requestId,
      prismaHeader = prismaHeader
    )

    result.assertFailingResponse(errorCode, errorCount, errorContains)
    result
  }

  /**
    * Execute a Query without Checks.
    */
  def errorExtractor(t: Throwable): Option[Int] = t match {
    case e: DeployApiError => Some(e.code)
    case _                 => None
  }

  def executeQueryWithAuthentication(query: String,
                                     variables: JsValue = JsObject.empty,
                                     requestId: String = "CombinedTestDatabase.requestId",
                                     prismaHeader: Option[String] = None): JsValue = {

    val schemaBuilder  = SchemaBuilder()(dependencies)
    val userContext    = SystemUserContext(None)
    val schema         = schemaBuilder(userContext)
    val renderedSchema = SchemaRenderer.renderSchema(schema)
    val errorHandler   = ErrorHandler(requestId, "", "", Vector.empty, query.stripMargin, variables, dependencies.reporter, errorCodeExtractor = errorExtractor)

    if (printSchema) println(renderedSchema)
    if (writeSchemaToFile) writeSchemaIntoFile(renderedSchema)

    val queryAst = QueryParser.parse(query.stripMargin).get
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

  def deploySchema(project: Project, schema: String): Project = deploySchema(project.id, schema)

  def deploySchema(projectId: String, schema: String): Project = {
    deployHelper(projectId, schema.stripMargin, Vector.empty)
    dependencies.projectPersistence.load(projectId).await.get
  }

  def deploySchemaThatMustSucceed(project: Project, schema: String, revision: Int, force: Boolean = false): JsValue = {
    val res = deployHelper(project.id, schema, Vector.empty, shouldFail = false, shouldWarn = false, force = force)
    require(res.pathAsDouble("data.deploy.migration.revision") == revision)
    res
  }

  def deploySchemaThatMustError(project: Project, schema: String, force: Boolean = false): JsValue = deploySchemaThatMustError(project.id, schema, force)

  def deploySchemaThatMustError(projectId: String, schema: String): JsValue = deploySchemaThatMustError(projectId, schema, force = false)

  def deploySchemaThatMustError(projectId: String, schema: String, force: Boolean): JsValue = {
    deployHelper(projectId, schema, Vector.empty, shouldFail = true, force = force)
  }

  def deploySchemaThatMustErrorWithCode(project: Project, schema: String, force: Boolean = false, errorCode: Int): JsValue = {
    deployHelper(project.id, schema, Vector.empty, shouldFail = true, force = force, errorCode = errorCode)
  }

  def deploySchemaThatMustWarn(project: Project, schema: String, force: Boolean = false): JsValue = {
    deployHelper(project.id, schema, Vector.empty, shouldFail = false, shouldWarn = true, force = force)
  }

  def deploySchemaThatMustWarnAndReturnProject(project: Project, schema: String, force: Boolean = false): Project = {
    deployHelper(project.id, schema, Vector.empty, shouldFail = false, shouldWarn = true, force = force)
    dependencies.projectPersistence.load(project.id).await.get
  }

  def deploySchemaThatMustFailAndWarn(project: Project, schema: String, force: Boolean = false): JsValue = {
    deployHelper(project.id, schema, Vector.empty, shouldFail = true, shouldWarn = true, force = force)
  }

  def deploySchema(name: String, stage: String, schema: String, secrets: Vector[String] = Vector.empty): (Project, Migration) = {
    val projectId = dependencies.projectIdEncoder.toEncodedString(List(name, stage))
    val result    = deployHelper(projectId, schema, secrets)
    val revision  = result.pathAsLong("data.deploy.migration.revision")
    (dependencies.projectPersistence.load(projectId).await.get, dependencies.migrationPersistence.byId(MigrationId(projectId, revision.toInt)).await.get)
  }

  private def deployHelper(projectId: String,
                           schema: String,
                           secrets: Vector[String],
                           shouldFail: Boolean = false,
                           shouldWarn: Boolean = false,
                           force: Boolean = false,
                           queryFailsCompletely: Boolean = false,
                           errorCode: Int = 0): JsValue = {

    val nameAndStage     = dependencies.projectIdEncoder.fromEncodedString(projectId)
    val name             = nameAndStage.name
    val stage            = nameAndStage.stage
    val formattedSchema  = JsString(schema.stripMargin).toString
    val secretsFormatted = JsArray(secrets.map(JsString)).toString()

    val queryString = s"""
                         |mutation {
                         |  deploy(input:{name: "$name", stage: "$stage", types: $formattedSchema, secrets: $secretsFormatted, force: $force}){
                         |    migration {
                         |      applied
                         |      revision
                         |    }
                         |    errors {
                         |      description
                         |    }
                         |    warnings{
                         |      description
                         |    }
                         |  }
                         |}
      """.stripMargin

    errorCode != 0 match {
      case true =>
        executeQueryThatMustFail(queryString, errorCode)

      case false =>
        val deployResult = query(queryString)
        deployResult.assertErrorsAndWarnings(shouldFail, shouldWarn)
        deployResult
    }
  }
}
