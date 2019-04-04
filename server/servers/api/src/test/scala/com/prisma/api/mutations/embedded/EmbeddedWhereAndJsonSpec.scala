package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedWhereAndJsonSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  // We don't support scalar filters on JSON fields at the moment
  override def doNotRun: Boolean = true

  "Using the same input in an update using where as used during creation of the item" should "work" in {

    val outerWhere = """"{\"stuff\": 1, \"nestedStuff\" : {\"stuff\": 2 } }""""
    val innerWhere = """"{\"stuff\": 2, \"nestedStuff\" : {\"stuff\": 1, \"nestedStuff\" : {\"stuff\": 2 } } }""""

    val project = SchemaDsl.fromStringV11() {
      """type Note{
        |   id: ID! @id
        |   outerString: String!
        |   outerJson: Json! @unique
        |   todos: [Todo]
        |}
        |
        |type Todo @embedded{
        |   innerString: String!
        |   innerJson: Json!
        |}"""
    }

    database.setup(project)

    val createResult = server.query(
      s"""mutation {
         |  createNote(
         |    data: {
         |      outerString: "Outer String"
         |      outerJson: $outerWhere
         |      todos: {
         |        create: [
         |        {innerString: "Inner String", innerJson: $innerWhere}
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
         |    where: { outerJson: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        updateMany: [
         |          { where: { innerJson: $innerWhere },data:{ innerString: "Changed Inner String"} }
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

    server.query(s"""query{note(where:{outerJson:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Changed Outer String"}}""")

  }
}
