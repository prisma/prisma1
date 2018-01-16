package com.prisma.api.queries

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class SingleItemQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {

  "the single item query" should "return null if the id does not exist" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      s"""{
         |  todo(where: {id: "non-existent-id"}){
         |    id
         |    title
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todo":null}}""")
  }

  "the single item query" should "work by id" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .executeQuerySimple(s"""mutation {
        |  createTodo(data: {title: "$title"}) {
        |    id
        |  }
        |}""".stripMargin,
                          project)
      .pathAsString("data.createTodo.id")

    val result = server.executeQuerySimple(s"""{
        |  todo(where: {id: "$id"}){
        |    id
        |    title
        |  }
        |}""".stripMargin,
                                           project)

    result.pathAsString("data.todo.title") should equal(title)
  }

  "the single item query" should "work by any unique field" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field_!("alias", _.String, isUnique = true)
    }
    database.setup(project)

    val title = "Hello World!"
    val alias = "my-alias"
    server.executeQuerySimple(
      s"""mutation {
         |  createTodo(data: {title: "$title", alias: "$alias"}) {
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    val result = server.executeQuerySimple(
      s"""{
          |  todo(where: {alias: "$alias"}){
          |    id
          |    title
          |  }
          |}""".stripMargin,
      project
    )

    result.pathAsString("data.todo.title") should equal(title)
  }
}
