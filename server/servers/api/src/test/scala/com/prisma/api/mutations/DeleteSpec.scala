package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.connector.postgresql.database.DatabaseQueryBuilder
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl() { schema =>
    schema.model("Todo").field_!("title", _.String, isUnique = true)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncateProjectTables(project)
  }

  "The delete  mutation" should "delete the item matching the where clause" in {
    createTodo("title1")
    createTodo("title2")
    todoAndRelayCountShouldBe(2)

    server.query(
      """mutation {
        |  deleteTodo(
        |    where: { title: "title1" }
        |  ){
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )

    todoAndRelayCountShouldBe(1)
  }

  "The delete  mutation" should "error if the where clause misses" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")
    todoAndRelayCountShouldBe(3)

    server.queryThatMustFail(
      """mutation {
        |  deleteTodo(
        |    where: { title: "does not exist" }
        |  ){
        |    id
        |  }
        |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = """No Node for the model Todo with value does not exist for title found."""
    )

    todoAndRelayCountShouldBe(3)
  }

  def todoAndRelayCountShouldBe(int: Int) = {
    val result = server.query(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size should be(int)

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(int))

  }

  def createTodo(title: String): Unit = {
    server.query(
      s"""mutation {
        |  createTodo(
        |    data: {
        |      title: "$title"
        |    }
        |  ) {
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )
  }
}
