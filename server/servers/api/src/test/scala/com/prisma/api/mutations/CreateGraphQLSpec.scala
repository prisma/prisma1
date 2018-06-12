package com.prisma.api.mutations

import java.util.UUID

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class CreateGraphQLSpec extends FlatSpec with Matchers with ApiSpecBase {
  "Creating an item with an id field of type UUID" should "work" in {
    val project = SchemaDsl.fromString() {
      s"""
         |type Todo {
         |  id: UUID! @unique
         |  title: String!
         |}
       """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  createTodo(data: { title: "the title" }){
        |    id
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.createTodo.title") should equal("the title")
    val theUUID = result.pathAsString("data.createTodo.id")
    UUID.fromString(theUUID) // should just not blow up
  }
}
