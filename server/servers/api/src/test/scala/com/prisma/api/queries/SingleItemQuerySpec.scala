package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers, WordSpecLike}

class SingleItemQuerySpec extends WordSpecLike with Matchers with ApiSpecBase {

  "should return null if the id does not exist" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val result = server.query(
      s"""{
         |  todo(where: {id: "5beea4aa6183dd734b2dbd9b"}){
         |    ...todoFields
         |  }
         |}
         |
         |fragment todoFields on Todo {
         |  id
         |  title
         |}
         |""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todo":null}}""")
  }

  "should work by id" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(s"""mutation {
        |  createTodo(data: {title: "$title"}) {
        |    id
        |  }
        |}""".stripMargin,
             project)
      .pathAsString("data.createTodo.id")

    val result = server.query(s"""{
        |  todo(where: {id: "$id"}){
        |    id
        |    title
        |  }
        |}""".stripMargin,
                              project)

    result.pathAsString("data.todo.title") should equal(title)
  }

  "should work by any unique field" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field_!("title", _.String).field_!("alias", _.String, isUnique = true)
    }
    database.setup(project)

    val title = "Hello World!"
    val alias = "my-alias"
    server.query(
      s"""mutation {
         |  createTodo(data: {title: "$title", alias: "$alias"}) {
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    val result = server.query(
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

  "should respect custom db names" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo @db(name: "my_table") {
        |  id: ID! @id
        |  title: String @db(name: "my_column")
        |}
      """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      s"""{
         |  todo(where: {id: "5beea4aa6183dd734b2dbd9b"}){
         |    ...todoFields
         |  }
         |}
         |
         |fragment todoFields on Todo {
         |  id
         |  title
         |}
         |""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"todo":null}}""")
  }
}
