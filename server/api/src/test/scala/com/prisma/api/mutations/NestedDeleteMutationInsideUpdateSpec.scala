package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.database.DatabaseQueryBuilder
import com.prisma.shared.project_dsl.SchemaDsl
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
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(1))

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
    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_RelayId").as[Int]) should be(Vector(1))

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

  "one2one relation both exist and are connected" should "be deletable by id through a nested mutation" in {
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

  "one2one relation both exist and are connected" should "be deletable by unique field through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String, isUnique = true)
      schema.model("Todo").field_!("title", _.String, isUnique = true).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "FirstUnique"
        |      todo: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      text: "FirstUnique"
         |    }
         |    data: {
         |      todo: {
         |        delete: {title: "the title"}
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

    val query2 = server.executeQuerySimple("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"FirstUnique"}]}}""")
  }

  "A nested delete in a one2one relation where both nodes exist but are not connected" should "fail completely" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String, isUnique = true)
      schema.model("Todo").field_!("title", _.String, isUnique = true).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "FirstUnique"
        |      todo: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )

    server.executeQuerySimple("""mutation {createNote(data: {text: "SecondUnique"}){id}}""", project)

    val result = server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      text: "SecondUnique"
         |    }
         |    data: {
         |      todo: {
         |        delete: {title: "the title"}
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3041,
      errorContains =
        "The relation TodoToNote has no Node for the model Note with value `SecondUnique` for text connected to a Node for the model Todo with value `the title` for title"
    )

    val query = server.executeQuerySimple("""{ todoes { title }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[{"title":"the title"}]}}""")

    val query2 = server.executeQuerySimple("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"FirstUnique"},{"text":"SecondUnique"}]}}""")
  }

  "A one2one relation" should "not do a nested delete by id if the nodes are not connected" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Note"
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

    val todoId2 = server.executeQuerySimple("""mutation {createTodo(data: { title: "the title2" }){id}}""", project).pathAsString("data.createTodo.id")

    val result = server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      todo: {
         |        delete: {id: "$todoId2"}
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3041,
      errorContains = "The relation TodoToNote has no Node for the model Note"
    )

    val query = server.executeQuerySimple("""{ todoes { title }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[{"title":"the title"},{"title":"the title2"}]}}""")

    val query2 = server.executeQuerySimple("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"Note"}]}}""")
  }

  "a one to one relation" should "not do a nested delete by id if the nested node does not exist" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Note"
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

    val result = server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {id: "$noteId"}
         |    data: {
         |      todo: {
         |        delete: {id: "DOES NOT EXISTS"}
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Todo with value DOES NOT EXISTS for id found."
    )

    val query = server.executeQuerySimple("""{ todoes { title }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[{"title":"the title"}]}}""")

    val query2 = server.executeQuerySimple("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"Note"}]}}""")
  }
}
