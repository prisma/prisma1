package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedWhereAndDateTimeSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Using the same input in an update using where as used during creation of the item" should "work" in {

    val outerWhere = """"2018-12-05T12:34:23.000Z""""
    val innerWhere = """"2019-12-05T12:34:23.000Z""""

    val project = SchemaDsl.fromString() {
      """type Note{
        |   id: ID! @unique
        |   outerString: String!
        |   outerDateTime: DateTime! @unique
        |   todos: [Todo]
        |}
        |
        |type Todo @embedded{
        |   innerString: String!
        |   innerDateTime: DateTime! @unique
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |  createNote(
         |    data: {
         |      outerString: "Outer String"
         |      outerDateTime: $outerWhere
         |      todos: {
         |        create: [
         |        {innerString: "Inner String", innerDateTime: $innerWhere}
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    server.query(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerDateTime: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: [{where: { innerDateTime: $innerWhere },data:{ innerString: "Changed Inner String"}}]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """,
      project
    )

    server.query(
      s"""query{note(where:{outerDateTime:$outerWhere}){outerString, outerDateTime}}""",
      project,
      dataContains = """{"note":{"outerString":"Changed Outer String","outerDateTime":"2018-12-05T12:34:23.000Z"}}"""
    )

  }

  "Using the same input in an update using where as used during creation of the item" should "work with the same time for inner and outer" in {

    val outerWhere = """"2018-01-03T11:27:38+00:00""""
    val innerWhere = """"2018-01-03T11:27:38+00:00""""

    val project = SchemaDsl.fromString() {
      """type Note{
        |   id: ID! @unique
        |   outerString: String!
        |   outerDateTime: DateTime! @unique
        |   todos: [Todo]
        |}
        |
        |type Todo @embedded{
        |   innerString: String!
        |   innerDateTime: DateTime! @unique
        |}"""

    }

    database.setup(project)

    val createResult = server.query(
      s"""mutation {
         |  createNote(
         |    data: {
         |      outerString: "Outer String"
         |      outerDateTime: $outerWhere
         |      todos: {
         |        create: [
         |        {innerString: "Inner String", innerDateTime: $innerWhere}
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    server.query(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerDateTime: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: [
         |        {where: { innerDateTime: $innerWhere },data:{ innerString: "Changed Inner String"}}
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project
    )

    server.query(s"""query{note(where:{outerDateTime:$outerWhere}){outerString}}""",
                 project,
                 dataContains = s"""{"note":{"outerString":"Changed Outer String"}}""")
  }

}
