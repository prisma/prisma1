package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedCreateMutationInsideCreateSpec extends FlatSpec with Matchers with ApiBaseSpec {

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

    val result = server
      .executeQuerySimple(
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

  "A nested create on a one to one relation" should "correctly assign violations to offending model and not partially execute" in {
    val project = SchemaDsl() { schema =>
      val user = schema.model("User").field_!("name", _.String).field("unique", _.String, isUnique = true)
      schema.model("Post").field_!("title", _.String).field("uniquePost", _.String, isUnique = true).oneToOneRelation("user", "post", user)
    }
    database.setup(project)

    server.executeQuerySimple(
      """mutation{
        |  createUser(data:{
        |    name: "Paul"
        |    unique: "uniqueUser"
        |    post: {create:{title: "test"    uniquePost: "uniquePost"}
        |    }
        |  })
        |    {id}
        |  }
      """.stripMargin,
      project
    )

    server.executeQuerySimple("query{users{id}}", project).pathAsSeq("data.users").length should be(1)
    server.executeQuerySimple("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)

    server.executeQuerySimpleThatMustFail(
      """mutation{
        |  createUser(data:{
        |    name: "Paul2"
        |    unique: "uniqueUser"
        |    post: {create:{title: "test2"    uniquePost: "uniquePost2"}
        |    }
        |  })
        |    {id}
        |  }
      """.stripMargin,
      project,
      errorCode = 3010,
      errorContains = "A unique constraint would be violated on User. Details: Field name = unique"
    )

    server.executeQuerySimple("query{users{id}}", project).pathAsSeq("data.users").length should be(1)
    server.executeQuerySimple("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)

    server.executeQuerySimpleThatMustFail(
      """mutation{
        |  createUser(data:{
        |    name: "Paul2"
        |    unique: "uniqueUser2"
        |    post: {create:{title: "test2"    uniquePost: "uniquePost"}
        |    }
        |  })
        |    {id}
        |  }
      """.stripMargin,
      project,
      errorCode = 3010,
      errorContains = "A unique constraint would be violated on Post. Details: Field name = uniquePost"
    )

    server.executeQuerySimple("query{users{id}}", project).pathAsSeq("data.users").length should be(1)
    server.executeQuerySimple("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)
  }

  "a deeply nested mutation" should "execute all levels of the mutation" in {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("name", _.String)
      val todo = schema.model("Todo").field_!("title", _.String)
      val tag  = schema.model("Tag").field_!("name", _.String)

      list.oneToManyRelation("todos", "list", todo)
      todo.oneToOneRelation("tag", "todo", tag)
    }
    database.setup(project)

    val mutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tag: {
        |            create: {
        |              name: "the tag"
        |            }
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    name
        |    todos {
        |      title
        |      tag {
        |        name
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val result = server.executeQuerySimple(mutation, project)
    result.pathAsString("data.createList.name") should equal("the list")
    result.pathAsString("data.createList.todos.[0].title") should equal("the todo")
    result.pathAsString("data.createList.todos.[0].tag.name") should equal("the tag")
  }

  "a required one2one relation" should "be creatable through a nested create mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("reqOnComment", _.String).field("optOnComment", _.String)
      schema.model("Todo").field_!("reqOnTodo", _.String).field("optOnTodo", _.String).oneToOneRelation_!("comments", "todo", comment)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {
        |      create: {reqOnTodo: "todo1"}
        |    }
        |  }){
        |    id
        |    todo{reqOnTodo}
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.createComment.todo.reqOnTodo"), "todo1")

    server.executeQuerySimpleThatMustFail(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {}
        |  }){
        |    id
        |    todo {
        |      reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project,
      errorCode = 3032,
      errorContains = "The field 'todo' on type 'Comment' is required. Performing this mutation would violate that constraint"
    )
  }

  "a required one2one relation" should "be creatable through a nested connected mutation" in {
    val project = SchemaDsl() { schema =>
      val todo = schema.model("Todo").field_!("reqOnTodo", _.String).field("optOnTodo", _.String)
      schema
        .model("Comment")
        .field_!("reqOnComment", _.String)
        .field("optOnComment", _.String)
        .oneToOneRelation_!("todo", "comment", todo, isRequiredOnOtherField = false)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {
        |      create: {reqOnTodo: "todo1"}
        |    }
        |  }){
        |    id
        |    todo{
        |       reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.createComment.todo.reqOnTodo"), "todo1")

    server.executeQuerySimple("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(1)
    server.executeQuerySimple("{ comments { id } }", project).pathAsSeq("data.comments").size should be(1)

    server.executeQuerySimpleThatMustFail(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {}
        |  }){
        |    id
        |    todo {
        |      reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project,
      errorCode = 3032,
      errorContains = "The field 'todo' on type 'Comment' is required. Performing this mutation would violate that constraint"
    )

    server.executeQuerySimple("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(1)
    server.executeQuerySimple("{ comments { id } }", project).pathAsSeq("data.comments").size should be(1)

    val todoId = server
      .executeQuerySimple(
        """
        |mutation {
        |  createTodo(data: {
        |       reqOnTodo: "todo2"
        |       }
        |       )
        |  {id}
        |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    server.executeQuerySimple("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(2)
    server.executeQuerySimple("{ comments { id } }", project).pathAsSeq("data.comments").size should be(1)

    server.executeQuerySimple(
      s"""
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {
        |      connect: {id: "$todoId"}
        |    }
        |  }){
        |    id
        |    todo{
        |       reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    server.executeQuerySimple("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(2)
    server.executeQuerySimple("{ comments { id } }", project).pathAsSeq("data.comments").size should be(2)

  }

}
