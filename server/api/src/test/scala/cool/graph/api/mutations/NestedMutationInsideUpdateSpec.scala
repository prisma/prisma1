package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a one to many relation" should "be creatable through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(data:{}){
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )
    val id = createResult.pathAsString("data.createTodo.id")

    val result = server.executeQuerySimple(
      s"""mutation {
        |  updateTodo(
        |    where: {
        |      id: "$id"
        |    }
        |    data:{
        |      comments: {
        |        create: [{text: "comment1"}, {text: "comment2"}]
        |      }
        |    }
        |  ){
        |    comments {
        |      text
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[{"text":"comment1"},{"text":"comment2"}]""")
  }

  "a many to one relation" should "be creatable through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createComment(data:{}){
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )
    val id = createResult.pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
      s"""
        |mutation {
        |  updateComment(
        |    where: {
        |      id: "$id"
        |    }
        |    data: {
        |      todo: {
        |        create: {title: "todo1"}
        |      }
        |    }
        |  ){
        |    id
        |    todo {
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.updateComment.todo.title"), "todo1")
  }
}
