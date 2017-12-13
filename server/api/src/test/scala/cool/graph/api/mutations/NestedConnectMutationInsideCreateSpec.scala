package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedConnectMutationInsideCreateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a many relation" should "be connectable through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val comment1Id = server.executeQuerySimple("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    val comment2Id = server.executeQuerySimple("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
      s"""
        |mutation {
        |  createTodo(data:{
        |    comments: {
        |      connect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
        |    }
        |  }){
        |    id
        |    comments {
        |      id
        |      text
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      actual = result.pathAsJsValue("data.createTodo.comments").toString,
      expected = s"""[{"id":"$comment1Id","text":"comment1"},{"id":"$comment2Id","text":"comment2"}]"""
    )
  }
}
