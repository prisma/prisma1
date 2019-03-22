package com.prisma.api.mutations

import com.prisma.{IgnoreMongo, IgnoreMySql}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project: Project = SchemaDsl.fromBuilder { schema =>
    schema.model("Todo").field_!("title", _.String, isUnique = true)
  }

  "The delete mutation" should "delete the item matching the where clause" in {
    database.setup(project)
    createTodo(project, "title1")
    createTodo(project, "title2")
    todoAndRelayCountShouldBe(project, 2)

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

    todoAndRelayCountShouldBe(project, 1)
  }

  "The delete  mutation" should "error if the where clause misses" in {
    database.setup(project)
    createTodo(project, "title1")
    createTodo(project, "title2")
    createTodo(project, "title3")
    todoAndRelayCountShouldBe(project, 3)

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

    todoAndRelayCountShouldBe(project, 3)
  }

  "the delete mutation" should "work when node ids are UUIDs" taggedAs (IgnoreMySql, IgnoreMongo) in {
    val project = SchemaDsl.fromString()(s"""
         |type Todo {
         |  id: UUID! @unique
         |  title: String
         |}
       """.stripMargin)

    database.setup(project)

    val id = createTodo(project, "1")
    todoAndRelayCountShouldBe(project, 1)

    server.query(
      s"""mutation {
        |  deleteTodo(where:{id: "$id"}){
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )
    todoAndRelayCountShouldBe(project, 0)
  }

  def todoAndRelayCountShouldBe(project: Project, int: Int) = {
    val result = server.query(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size should be(int)

    ifConnectorIsActiveAndNotSqliteNative { dataResolver(project).countByTable("_RelayId").await should be(int) }
  }

  def createTodo(project: Project, title: String) = {
    server
      .query(
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
      .pathAsString("data.createTodo.id")
  }
}
