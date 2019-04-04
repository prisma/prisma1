package com.prisma.api.mutations

import java.util.UUID

import com.prisma.{IgnoreMongo, IgnoreMySql}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UUIDCreateGraphQLSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Creating an item with an id field of type UUID" should "work" taggedAs (IgnoreMySql, IgnoreMongo) in {
    val project = SchemaDsl.fromStringV11() {
      s"""
         |type Todo {
         |  id: UUID! @id
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

  "Fetching a UUID field that is null" should "work" taggedAs (IgnoreMySql, IgnoreMongo) in {
    val project = SchemaDsl.fromStringV11() {
      s"""
         |type TableA {
         |    id: UUID! @id
         |    name: String!
         |    b: UUID @unique
         |}""".stripMargin
    }
    database.setup(project)

    server.query("""mutation {createTableA(data: {name:"testA"}){id}}""", project)

    server.query("""query {tableAs {name, b}}""", project).toString should be("""{"data":{"tableAs":[{"name":"testA","b":null}]}}""")
  }
}
