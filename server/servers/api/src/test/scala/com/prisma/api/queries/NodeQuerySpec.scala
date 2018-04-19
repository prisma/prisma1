package com.prisma.api.queries

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import slick.jdbc.MySQLProfile.api._

class NodeQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {

  "the node query" should "return null if the id does not exist" in {
    val project = SchemaDsl.fromString() {
      """
        |type Todo {
        |  id: ID!
        |  title: String!
        |}
      """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      s"""{
         |  node(id: "non-existent-id"){
         |    id
         |    ... on Todo {
         |      title
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"node":null}}""")
  }

  "the node query" should "work if the given id exists" in {
    val project = SchemaDsl.fromString() {
      """
        |type Todo {
        |  id: ID!
        |  title: String!
        |}
      """.stripMargin
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(
        s"""mutation {
        |  createTodo(data: {title: "$title"}) {
        |    id
        |  }
        |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    val result = server.query(
      s"""{
        |  node(id: "$id"){
        |    id
        |    ... on Todo {
        |      title
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    result.pathAsString("data.node.title") should equal(title)
  }

  "the node query" should "work if the model name changed and the stableRelayIdentifier is the same" in {
    val project = SchemaDsl.fromString() {
      """
        |type Todo {
        |  id: ID!
        |  title: String!
        |}
      """.stripMargin
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    val model                       = project.schema.getModelByName_!("Todo")
    val updatedModel                = model.copy(name = "TodoNew")
    val projectWithUpdatedModelName = project.copy(schema = project.schema.copy(models = List(updatedModel)))

    model.stableIdentifier should equal(updatedModel.stableIdentifier) // this invariant must be guaranteed by the SchemaInferer

    // update table name of Model
    database.runDbActionOnClientDb(sqlu"""ALTER Table "#${project.id}"."Todo" RENAME TO "TodoNew";""")

    val result = server.query(
      s"""{
         |  node(id: "$id"){
         |    id
         |    ... on TodoNew {
         |      title
         |    }
         |  }
         |}""".stripMargin,
      projectWithUpdatedModelName
    )

    result.pathAsString("data.node.title") should equal(title)
  }
}
