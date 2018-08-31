package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class RawAccessSpec extends FlatSpec with Matchers with ApiSpecBase {
  val project: Project = SchemaDsl.fromBuilder { schema =>
    schema.model("Todo").field("title", _.String, isUnique = true)
  }
  val schemaName = project.id
  val model      = project.schema.getModelByName_!("Todo")

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  "the simplest query Select 1" should "work" in {
    val result = server.query(
      """mutation {
        |  executeRaw(
        |    query: "Select 1"
        |  )
        |}
          """.stripMargin,
      project
    )

    result.pathAsJsValue("data.executeRaw") should equal(s"""[{"?column?":1}]""".parseJson)
  }

  "querying model tables" should "work" in {
    val id1 = createTodo(project, "title1")
    val id2 = createTodo(project, null)

    val result = server.query(
      s"""mutation {
        |  executeRaw(
        |    query: "Select * from \\"$schemaName\\".\\"${model.dbName}\\""
        |  )
        |}
      """.stripMargin,
      project
    )

    result.pathAsJsValue("data.executeRaw") should equal(s"""[{"id":"$id1","title":"title1"},{"id":"$id2","title":null}]""".parseJson)
  }

  "inserting into a model table" should "work" in {
    val insertResult = server.query(
      s"""mutation {
         |  executeRaw(
         |    query: "INSERT INTO \\"$schemaName\\".\\"${model.dbName}\\" VALUES ('id1', 'title1'),('id2', 'title2')"
         |  )
         |}
      """.stripMargin,
      project
    )

    insertResult.pathAsJsValue("data.executeRaw") should equal("2".parseJson)

    val readResult = server.query(
      s"""mutation {
         |  executeRaw(
         |    query: "Select * from \\"$schemaName\\".\\"${model.dbName}\\""
         |  )
         |}
      """.stripMargin,
      project
    )
    readResult.pathAsJsValue("data.executeRaw") should equal(s"""[{"id":"id1","title":"title1"},{"id":"id2","title":"title2"}]""".parseJson)
  }

  def createTodo(project: Project, title: String) = {
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
