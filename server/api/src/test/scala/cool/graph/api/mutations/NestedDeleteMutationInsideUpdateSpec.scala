package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedDeleteMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a one to many relation" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1"}, {text: "comment2"}]
        |      }
        |    }
        |  ){ 
        |    id 
        |    comments { id }
        |  } 
        |}""".stripMargin,
      project
    )

    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")
    val comment2Id = createResult.pathAsString("data.createTodo.comments.[1].id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        delete: [{id: "$comment1Id"}, {id: "$comment2Id"}]
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

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[]""")

    val query = server.executeQuerySimple("""{ comments { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"comments":[]}}""")
  }

  "a one to many relation" should "be deletable by any unique argument through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String).field_!("alias", _.String, isUnique = true)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1", alias: "alias1"}, {text: "comment2", alias: "alias2"}]
        |      }
        |    }
        |  ){
        |    id
        |    comments { id }
        |  }
        |}""".stripMargin,
      project
    )
    val todoId = createResult.pathAsString("data.createTodo.id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        delete: [{alias: "alias1"}, {alias: "alias2"}]
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

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[]""")

    val query = server.executeQuerySimple("""{ comments { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"comments":[]}}""")
  }

  "a many to one relation" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1"}]
        |      }
        |    }
        |  ){
        |    id
        |    comments { id }
        |  }
        |}""".stripMargin,
      project
    )
    val todoId    = createResult.pathAsString("data.createTodo.id")
    val commentId = createResult.pathAsString("data.createTodo.comments.[0].id")

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateComment(
         |    where: {
         |      id: "$commentId"
         |    }
         |    data: {
         |      todo: {
         |        delete: {id: "$todoId"}
         |      }
         |    }
         |  ){
         |    todo {
         |      id
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.updateComment").toString, """{"todo":null}""")

    val query = server.executeQuerySimple("""{ todoes { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")
  }

  "a one to one relation" should "be disconnectable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation { 
        |  createNote(
        |    data: {
        |      todo: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){ 
        |    id
        |    todo { id }
        |  } 
        |}""".stripMargin,
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")
    val todoId = createResult.pathAsString("data.createNote.todo.id")

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      todo: {
         |        delete: {id: "$todoId"}
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.updateNote").toString, """{"todo":null}""")

    val query = server.executeQuerySimple("""{ todoes { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")
  }
}
