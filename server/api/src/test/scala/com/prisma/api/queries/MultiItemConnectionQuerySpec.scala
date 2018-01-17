package com.prisma.api.queries

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MultiItemConnectionQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {

  "the connection query" should "return empty edges" in {

    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      s"""{
         |  todoesConnection{
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todoesConnection":{"edges":[]}}}""")
  }

  "the connection query" should "return single node" in {
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
         |  todoesConnection{
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todoesConnection":{"edges":[{"node":{"title":"Hello World!"}}]}}}""")
  }

  "the connection query" should "filter by any field" in {
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
         |  todoesConnection(where: {title: "INVALID"}){
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
        project
      )
      .toString should equal("""{"data":{"todoesConnection":{"edges":[]}}}""")

    server
      .executeQuerySimple(
        s"""{
         |  todoesConnection(where: {title: "${title}"}){
         |    edges {
         |      node {
         |        title
         |      }
         |    }
         |  }
         |}""".stripMargin,
        project
      )
      .toString should equal("""{"data":{"todoesConnection":{"edges":[{"node":{"title":"Hello World!"}}]}}}""")
  }
}
