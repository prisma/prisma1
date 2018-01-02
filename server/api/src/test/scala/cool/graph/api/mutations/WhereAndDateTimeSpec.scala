package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.gc_values.DateTimeGCValue
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}

class WhereAndDateTimeSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "Using the same input in an update using where as used during creation of the item" should "work" in {

    val outerWhere = """"2018""""
    val innerWhere = """"2019""""

    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("outerString", _.String).field("outerDateTime", _.DateTime, isUnique = true)
      schema.model("Todo").field_!("innerString", _.String).field("innerDateTime", _.DateTime, isUnique = true).manyToManyRelation("notes", "todos", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
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

    server.executeQuerySimple(
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

    val res = server.executeQuerySimple(s"""query{note(where:{outerDateTime:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Changed Outer String"}}""")
    val res2 = server.executeQuerySimple(s"""query{todo(where:{innerDateTime:$innerWhere}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Changed Inner String"}}""")

  println(res)

  }

}

