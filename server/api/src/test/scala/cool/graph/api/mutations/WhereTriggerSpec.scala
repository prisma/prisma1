package cool.graph.api.mutations

import java.sql.SQLException

import cool.graph.api.ApiBaseSpec
import cool.graph.api.database.DatabaseMutationBuilder
import cool.graph.gc_values.StringGCValue
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class WhereTriggerSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a many to many relation" should "handle null in unique fields" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String, isUnique = true)
      schema.model("Todo").field_!("title", _.String, isUnique = true).field("unique", _.String, isUnique = true).manyToManyRelation("notes", "todos", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Some Text"
        |      todos:
        |      {
        |       create: [{ title: "the title"},{ title: "the other title"}]
        |      }
        |    }
        |  ){
        |    id
        |    todos { id }
        |  }
        |}""".stripMargin,
      project
    )

    val noteModel = project.getModelByName_!("Note")

   try {
     database.runDbActionOnClientDb(DatabaseMutationBuilder.whereFailureTrigger(project, NodeSelector(noteModel, "text", StringGCValue("Some Text 2"))))
   } catch {
     case e: SQLException =>
       println(e.getErrorCode)
       println(e.getMessage)
   }

    database.runDbActionOnClientDb(DatabaseMutationBuilder.whereFailureTrigger(project, NodeSelector(noteModel, "text", StringGCValue("Some Text 2"))))


    //
//    val result = server.executeQuerySimpleThatMustFail(
//      s"""
//         |mutation {
//         |  updateNote(
//         |    where: {
//         |      text: "Some Text"
//         |    }
//         |    data: {
//         |      text: "Some Changed Text"
//         |      todos: {
//         |        update: {
//         |          where: {unique: null},
//         |          data:{title: "updated title"}
//         |        }
//         |      }
//         |    }
//         |  ){
//         |    text
//         |    todos {
//         |      title
//         |    }
//         |  }
//         |}
//      """.stripMargin,
//      project,
//      errorCode = 3040,
//      errorContains = "You provided an invalid argument for the where selector on Todo."
//    )
  }

}
