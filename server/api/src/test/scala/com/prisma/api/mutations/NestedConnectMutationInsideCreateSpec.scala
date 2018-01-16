package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedConnectMutationInsideCreateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a many relation" should "be connectable through a nested mutation by id" in {
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

  "a many relation" should "throw a proper error if connected by wrong id" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{id: "DoesNotExist"}]
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
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Comment with value DoesNotExist for id found."
    )
  }

  "a many relation" should "throw a proper error if connected by wrong id the other way around" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  createComment(data:{
         |    text: "bla"
         |    todo: {
         |      connect: {id: "DoesNotExist"}
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Todo with value DoesNotExist for id found."
    )
  }

  "a many relation" should "throw a proper error if the id of a wrong model is provided" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val comment1Id = server.executeQuerySimple("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    val comment2Id = server.executeQuerySimple("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val todoId = server
      .executeQuerySimple(
        s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{id: "$todoId"}]
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = s"No Node for the model Comment with value $todoId for id found."
    )

  }

  "a many relation" should "be connectable through a nested mutation by any unique argument" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String).field_!("alias", _.String, isUnique = true)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val comment1Alias = server
      .executeQuerySimple("""mutation { createComment(data: {text: "text comment1", alias: "comment1"}){ alias } }""", project)
      .pathAsString("data.createComment.alias")
    val comment2Alias = server
      .executeQuerySimple("""mutation { createComment(data: {text: "text comment2", alias: "comment2"}){ alias } }""", project)
      .pathAsString("data.createComment.alias")

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{alias: "$comment1Alias"}, {alias: "$comment2Alias"}]
         |    }
         |  }){
         |    id
         |    comments {
         |      alias
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      actual = result.pathAsJsValue("data.createTodo.comments").toString,
      expected = s"""[{"alias":"$comment1Alias","text":"text comment1"},{"alias":"$comment2Alias","text":"text comment2"}]"""
    )
  }

  "a many relation" should "be connectable through a nested mutation by any unique argument in the opposite direction" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment")
      schema.model("Todo").field_!("title", _.String).oneToManyRelation("comments", "todo", comment).field_!("alias", _.String, isUnique = true)
    }
    database.setup(project)

    val todoAlias = server
      .executeQuerySimple("""mutation { createTodo(data: {title: "the title", alias: "todo1"}){ alias } }""", project)
      .pathAsString("data.createTodo.alias")

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  createComment(
         |    data: {
         |      todo: {
         |        connect: { alias: "$todoAlias"}
         |      }
         |    }
         |  )
         |  {
         |    todo {
         |      alias
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      actual = result.pathAsJsValue("data.createComment.todo").toString,
      expected = s"""{"alias":"$todoAlias","title":"the title"}"""
    )
  }
}
