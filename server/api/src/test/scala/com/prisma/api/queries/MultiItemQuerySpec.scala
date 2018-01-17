package com.prisma.api.queries

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MultiItemQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {

  "the multi item query" should "return empty list" in {

    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      s"""{
         |  todoes {
         |    title
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todoes":[]}}""")
  }

  "the multi item query" should "return single node" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .executeQuerySimple(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    val result = server.executeQuerySimple(
      s"""{
         |  todoes {
         |    title
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todoes":[{"title":"Hello World!"}]}}""")
  }

  "the multi item query" should "filter by any field" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .executeQuerySimple(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    server
      .executeQuerySimple(
        s"""{
           |  todoes(where: {title: "INVALID"}) {
           |    title
           |  }
           |}""".stripMargin,
        project
      )
      .toString should equal("""{"data":{"todoes":[]}}""")

    server
      .executeQuerySimple(
        s"""{
           |  todoes(where: {title: "${title}"}) {
           |    title
           |  }
           |}""".stripMargin,
        project
      )
      .toString should equal("""{"data":{"todoes":[{"title":"Hello World!"}]}}""")
  }
}
