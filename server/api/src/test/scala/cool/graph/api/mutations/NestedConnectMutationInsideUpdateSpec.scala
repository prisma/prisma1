package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedConnectMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a one to many relation" should "be connectable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val todoId     = server.executeQuerySimple("""mutation { createTodo(data: {}){ id } }""", project).pathAsString("data.createTodo.id")
    val comment1Id = server.executeQuerySimple("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    val comment2Id = server.executeQuerySimple("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        connect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
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

  "a one to many relation" should "be connectable by any unique argument through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String).field_!("alias", _.String, isUnique = true)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val todoId = server.executeQuerySimple("""mutation { createTodo(data: {}){ id } }""", project).pathAsString("data.createTodo.id")
    server.executeQuerySimple("""mutation { createComment(data: {text: "comment1", alias: "alias1"}){ id } }""", project).pathAsString("data.createComment.id")
    server.executeQuerySimple("""mutation { createComment(data: {text: "comment2", alias: "alias2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        connect: [{alias: "alias1"}, {alias: "alias2"}]
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

  "a many to one relation" should "be connectable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val commentId = server.executeQuerySimple("""mutation { createComment(data: {}){ id } }""", project).pathAsString("data.createComment.id")
    val todoId    = server.executeQuerySimple("""mutation { createTodo(data: { title: "the title" }){ id } }""", project).pathAsString("data.createTodo.id")

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateComment(
         |    where: {
         |      id: "$commentId"
         |    }
         |    data: {
         |      todo: {
         |        connect: {id: "$todoId"}
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
    mustBeEqual(result.pathAsString("data.updateComment.todo.title"), "the title")
  }

  "a one to one relation" should "be connectable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val noteId = server.executeQuerySimple("""mutation { createNote(data: {}){ id } }""", project).pathAsString("data.createNote.id")
    val todoId = server.executeQuerySimple("""mutation { createTodo(data: { title: "the title" }){ id } }""", project).pathAsString("data.createTodo.id")

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      todo: {
         |        connect: {id: "$todoId"}
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
    mustBeEqual(result.pathAsString("data.updateNote.todo.title"), "the title")
  }
}
