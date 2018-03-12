package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class WhereAndJsonSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "Using the same input in an update using where as used during creation of the item" should "work" in {

    val outerWhere = """"{\"stuff\": 1, \"nestedStuff\" : {\"stuff\": 2 } }""""
    val innerWhere = """"{\"stuff\": 2, \"nestedStuff\" : {\"stuff\": 1, \"nestedStuff\" : {\"stuff\": 2 } } }""""

    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("outerString", _.String).field("outerJson", _.Json, isUnique = true)
      schema.model("Todo").field_!("innerString", _.String).field("innerJson", _.Json, isUnique = true).manyToManyRelation("notes", "todos", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
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

    server.executeQuerySimple(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerJson: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: [
         |        {where: { innerJson: $innerWhere },data:{ innerString: "Changed Inner String"}}
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

    server.executeQuerySimple(s"""query{note(where:{outerJson:$outerWhere}){outerString}}""",
                              project,
                              dataContains = s"""{"note":{"outerString":"Changed Outer String"}}""")
    server.executeQuerySimple(s"""query{todo(where:{innerJson:$innerWhere}){innerString}}""",
                              project,
                              dataContains = s"""{"todo":{"innerString":"Changed Inner String"}}""")

  }
}
