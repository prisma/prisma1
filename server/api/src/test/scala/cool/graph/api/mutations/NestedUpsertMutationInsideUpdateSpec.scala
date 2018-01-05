package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedUpsertMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a one to many relation" should "be upsertable by id through a nested mutation" in {
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

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "$comment1Id"}, update: {text: "update comment1"}, create: {text: "irrelevant"}},
         |          {where: {id: "non-existent-id"}, update: {text: "irrelevant"}, create: {text: "new comment3"}},
         |        ]
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

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """comment2""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[2].text").toString, """new comment3""")
  }

  "a one to many relation" should "only update nodes that are connected" in {
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
    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")

    val commentResult = server.executeQuerySimple(
      """mutation {
        |  createComment(
        |    data: {
        |      text: "comment2"
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )
    val comment2Id = commentResult.pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "$comment1Id"}, update: {text: "update comment1"}, create: {text: "irrelevant"}},
         |          {where: {id: "$comment2Id"}, update: {text: "irrelevant"}, create: {text: "new comment3"}},
         |        ]
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

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """new comment3""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation" in {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("name", _.String)
      val todo = schema.model("Todo").field_!("title", _.String)
      val tag  = schema.model("Tag").field_!("name", _.String)

      list.oneToManyRelation("todos", "list", todo)
      todo.oneToManyRelation("tags", "todo", tag)
    }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tags: {
        |            create: [
        |              {name: "the tag"}
        |            ]
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    id
        |    todos {
        |      id
        |      tags {
        |        id
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val createResult = server.executeQuerySimple(createMutation, project)
    val listId       = createResult.pathAsString("data.createList.id")
    val todoId       = createResult.pathAsString("data.createList.todos.[0].id")
    val tagId        = createResult.pathAsString("data.createList.todos.[0].tags.[0].id")

    val updateMutation =
      s"""
         |mutation  {
         |  updateList(
         |    where: {
         |      id: "$listId"
         |    }
         |    data: {
         |      todos: {
         |        upsert: [
         |          {
         |            where: { id: "$todoId" }
         |            create: { title: "irrelevant" }
         |            update: {
         |              tags: {
         |                upsert: [
         |                  {
         |                    where: { id: "$tagId" }
         |                    update: { name: "updated tag" }
         |                    create: { name: "irrelevant" }
         |                  },
         |                  {
         |                    where: { id: "non-existent-id" }
         |                    update: { name: "irrelevant" }
         |                    create: { name: "new tag" }
         |                  },
         |                ]
         |              }
         |            }
         |          }
         |        ]
         |      }
         |    }
         |  ) {
         |    name
         |    todos {
         |      title
         |      tags {
         |        name
         |      }
         |    }
         |  }
         |}
      """.stripMargin

    val result = server.executeQuerySimple(updateMutation, project)
    result.pathAsString("data.updateList.todos.[0].tags.[0].name") should equal("updated tag")
    result.pathAsString("data.updateList.todos.[0].tags.[1].name") should equal("new tag")
  }
}
