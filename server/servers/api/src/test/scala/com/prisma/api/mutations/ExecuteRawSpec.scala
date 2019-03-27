package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.jdbc.impl.JdbcDatabaseMutactionExecutor
import com.prisma.api.connector.sqlite.native.SQLiteDatabaseMutactionExecutor
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, RawAccessCapability}
import com.prisma.shared.models.{ConnectorCapability, Project}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.jooq.Query
import org.jooq.conf.{ParamType, Settings}
import org.jooq.impl.DSL
import org.jooq.impl.DSL.{field, name, table}
import org.scalatest.{Matchers, WordSpecLike}
import play.api.libs.json.{JsString, JsValue}
import sangria.util.StringUtil

class ExecuteRawSpec extends WordSpecLike with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability, RawAccessCapability)

  val project: Project = SchemaDsl.fromBuilder { schema =>
    schema.model("Todo").field("title", _.String)
  }
  val schemaName = project.dbName
  val model      = project.schema.getModelByName_!("Todo")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncateProjectTables(project)
  }

  lazy val slickDatabase = testDependencies.databaseMutactionExecutor match {
    case m: JdbcDatabaseMutactionExecutor   => m.slickDatabase
    case m: SQLiteDatabaseMutactionExecutor => m.slickDatabaseArg
  }

  lazy val isMySQL     = slickDatabase.isMySql
  lazy val isPostgres  = slickDatabase.isPostgres
  lazy val isSQLite    = slickDatabase.isSQLite
  lazy val sql         = DSL.using(slickDatabase.dialect, new Settings().withRenderFormatted(true))
  lazy val modelTable  = table(name(schemaName, model.dbName))
  lazy val idColumn    = model.idField_!.dbName
  lazy val titleColumn = model.getScalarFieldByName_!("title").dbName

  "the simplest query Select 1 should work" in {
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

  "querying model tables should work" in {
    val id1 = createTodo("title1")
    val id2 = createTodo(null)

    val result = executeRaw(sql.select().from(modelTable))

    result.pathAsJsValue("data.executeRaw") should equal(
      s"""[{"$idColumn":"$id1","$titleColumn":"title1"},{"$idColumn":"$id2","$titleColumn":null}]""".parseJson)
  }

  "inserting into a model table should work" in {
    val insertResult = executeRaw(sql.insertInto(modelTable).columns(field(idColumn), field(titleColumn)).values("id1", "title1").values("id2", "title2"))
    insertResult.pathAsJsValue("data.executeRaw") should equal("2".parseJson)

    val readResult = executeRaw(sql.select().from(modelTable))
    readResult.pathAsJsValue("data.executeRaw") should equal(
      s"""[{"$idColumn":"id1","$titleColumn":"title1"},{"$idColumn":"id2","$titleColumn":"title2"}]""".parseJson)
  }

  "querying model tables with alias should work" in {
    val id1 = createTodo("title1")
    val id2 = createTodo(null)

    val result = executeRaw(sql.select(field(titleColumn).as("aliasedTitle")).from(modelTable))

    result.pathAsJsValue("data.executeRaw") should equal(s"""[{"aliasedTitle":"title1"},{"aliasedTitle":null}]""".parseJson)
  }

  "querying the same column name twice but aliasing it should work" in {
    val id1 = createTodo("title1")
    val id2 = createTodo(null)

    val result = executeRaw(sql.select(field(titleColumn).as("ALIASEDTITLE"), field(titleColumn)).from(modelTable))

    result.pathAsJsValue("data.executeRaw") should equal(
      s"""[{"ALIASEDTITLE":"title1","$titleColumn":"title1"},{"ALIASEDTITLE":null,"$titleColumn":null}]""".parseJson)
  }

  "postgres arrays should work" in {
    if (isPostgres) {
      val query =
        """
          |SELECT
          |    array_agg(columnInfos.attname) as postgres_array
          |FROM
          |    pg_attribute columnInfos;
        """.stripMargin

      val result = server.query(
        s"""mutation {
           |  executeRaw(
           |    query: "${StringUtil.escapeString(query)}"
           |  )
           |}
        """.stripMargin,
        project
      )

      val postgresArray = result.pathAsJsArray("data.executeRaw").value.head.pathAsJsArray("postgres_array").value
      postgresArray should not(be(empty))
      val allAreStrings = postgresArray.forall {
        case _: JsString => true
        case _           => false
      }
      allAreStrings should be(true)
    }
  }

  "syntactic errors should bubble through to the user" in {
    val (errorCode, errorContains) = () match {
      case _ if isPostgres => (0, "syntax error at end of input")
      case _ if isMySQL    => (1064, "check the manual that corresponds to your MySQL server version for the right syntax to use near")
      case _ if isSQLite   => (1, "incomplete input")
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

  "other errors should also bubble through to the user" in {
    val id = createTodo("title")
    val (errorCode, errorContains) = () match {
      case _ if isPostgres => (0, "duplicate key value violates unique constraint")
      case _ if isMySQL    => (1062, "Duplicate entry")
      case _ if isSQLite   => (19, "Abort due to constraint violation (UNIQUE constraint failed: Todo.id)")
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
