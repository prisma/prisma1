package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.{ConnectorCapability, Project}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.jooq.Query
import org.jooq.conf.{ParamType, Settings}
import org.jooq.impl.DSL
import org.jooq.impl.DSL.{field, name, table}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.JsValue
import sangria.util.StringUtil

class RawAccessSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def doNotRunForPrototypes: Boolean                   = true
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  val project: Project = SchemaDsl.fromBuilder { schema =>
    schema.model("Todo").field("title", _.String)
  }
  val schemaName = project.id
  val model      = project.schema.getModelByName_!("Todo")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncateProjectTables(project)
  }

  lazy val slickDatabase = testDependencies.databaseMutactionExecutor.asInstanceOf[JdbcDatabaseMutactionExecutor].slickDatabase
  lazy val isMySQL       = slickDatabase.isMySql
  lazy val isPostgres    = slickDatabase.isPostgres
  lazy val sql           = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))
  lazy val modelTable    = table(name(schemaName, model.dbName))
  lazy val idColumn      = model.idField_!.dbName
  lazy val titleColumn   = model.getScalarFieldByName_!("title").dbName

  "the simplest query Select 1" should "work" in {
    val result = server.query(
      """mutation {
        |  executeRaw(
        |    database: default
        |    query: "Select 1"
        |  )
        |}
          """.stripMargin,
      project
    )

    val columnName = if (isPostgres) "?column?" else "1"
    result.pathAsJsValue("data.executeRaw") should equal(s"""[{"$columnName":1}]""".parseJson)
  }

  "querying model tables" should "work" in {
    val id1 = createTodo("title1")
    val id2 = createTodo(null)

    val result = executeRaw(sql.select().from(modelTable))

    result.pathAsJsValue("data.executeRaw") should equal(
      s"""[{"$idColumn":"$id1","$titleColumn":"title1"},{"$idColumn":"$id2","$titleColumn":null}]""".parseJson)
  }

  "inserting into a model table" should "work" in {
    val insertResult = executeRaw(sql.insertInto(modelTable).columns(field(idColumn), field(titleColumn)).values("id1", "title1").values("id2", "title2"))
    insertResult.pathAsJsValue("data.executeRaw") should equal("2".parseJson)

    val readResult = executeRaw(sql.select().from(modelTable))
    readResult.pathAsJsValue("data.executeRaw") should equal(
      s"""[{"$idColumn":"id1","$titleColumn":"title1"},{"$idColumn":"id2","$titleColumn":"title2"}]""".parseJson)
  }

  "syntactic errors" should "bubble through to the user" in {
    val errorCode = if (isPostgres) 0 else 1064
    val errorContains = if (isPostgres) {
      "ERROR: syntax error at end of input"
    } else {
      "You have an error in your SQL syntax; check the manual that corresponds to your MySQL server version for the right syntax to use near"
    }
    server.queryThatMustFail(
      s"""mutation {
         |  executeRaw(
         |    query: "Select * from "
         |  )
         |}
      """.stripMargin,
      project,
      errorCode = errorCode,
      errorContains = errorContains
    )
  }

  "other errors" should "also bubble through to the user" in {
    val id        = createTodo("title")
    val errorCode = if (isPostgres) 0 else 1062
    val errorContains = if (isPostgres) {
      "ERROR: duplicate key value violates unique constraint"
    } else {
      "Duplicate entry"
    }
    executeRawThatMustFail(
      sql.insertInto(modelTable).columns(field(idColumn), field(titleColumn)).values(id, "irrelevant"),
      errorCode = errorCode,
      errorContains = errorContains
    )
  }

  def executeRaw(query: Query): JsValue = {
    server.query(
      s"""mutation {
         |  executeRaw(
         |    query: "${queryString(query)}"
         |  )
         |}
      """.stripMargin,
      project
    )
  }

  def executeRawThatMustFail(query: Query, errorCode: Int, errorContains: String): JsValue = {
    server.queryThatMustFail(
      s"""mutation {
         |  executeRaw(
         |    query: "${queryString(query)}"
         |  )
         |}
      """.stripMargin,
      project,
      errorCode = errorCode,
      errorContains = errorContains
    )
  }

  def queryString(query: Query): String = StringUtil.escapeString(query.getSQL(ParamType.INLINED))

  def createTodo(title: String) = {
    val finalTitle = Option(title).map(s => s""""$s"""").orNull
    server
      .query(
        s"""mutation {
           |  createTodo(
           |    data: {
           |      title: $finalTitle
           |    }
           |  ) {
           |    id
           |  }
           |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")
  }
}
