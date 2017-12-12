package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedMutationInsideCreateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a one to many relation" should "be creatable through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      """
        |mutation {
        |  createTodo(data:{
        |    comments: {
        |      create: [{text: "comment1"}, {text: "comment2"}]
        |    }
        |  }){
        |    id
        |    comments {
        |      text
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.createTodo.comments").toString, """[{"text":"comment1"},{"text":"comment2"}]""")
  }

  "a many to one relation" should "be creatable through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      """
        |mutation {
        |  createComment(data: {
        |    text: "comment1"
        |    todo: {
        |      create: {title: "todo1"}
        |    }
        |  }){
        |    id
        |    todo {
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.createComment.todo.title"), "todo1")
  }

  "a many to many relation" should "creatable through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val tag = schema.model("Tag").field_!("name", _.String)
      schema.model("Todo").field_!("title", _.String).manyToManyRelation("tags", "todos", tag)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      """
        |mutation {
        |  createTodo(data:{
        |    title: "todo1"
        |    tags: {
        |      create: [{name: "tag1"}, {name: "tag2"}]
        |    }
        |  }){
        |    id
        |    tags {
        |      name
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.createTodo.tags").toString, """[{"name":"tag1"},{"name":"tag2"}]""")

    val result2 = server.executeQuerySimple(
      """
        |mutation {
        |  createTag(data:{
        |    name: "tag1"
        |    todos: {
        |      create: [{title: "todo1"}, {title: "todo2"}]
        |    }
        |  }){
        |    id
        |    todos {
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result2.pathAsJsValue("data.createTag.todos").toString, """[{"title":"todo1"},{"title":"todo2"}]""")
  }
}
