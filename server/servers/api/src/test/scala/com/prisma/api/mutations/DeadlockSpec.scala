package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.Future

class DeadlockSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {

  import testDependencies.system.dispatcher

  "creating many items" should "not cause deadlocks" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment).field("a", _.String)
    }
    database.setup(project)

    def exec(i: Int) =
      Future(
        server.query(
          s"""mutation {
             |  createTodo(
             |    data:{
             |      a: "a"
             |    }
             |  ){
             |    a
             |  }
             |}
      """.stripMargin,
          project
        )
      )

    Future.traverse(0 to 50)((i) => exec(i)).await(seconds = 30)
  }

  "updating single item many times" should "not cause deadlocks" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment).field("a", _.String)
    }
    database.setup(project)

    val createResult = server.query(
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

    def exec(i: Int) =
      server.queryAsync(
        s"""mutation {
             |  updateTodo(
             |    where: { id: "${todoId}" }
             |    data:{
             |      a: "${i}"
             |    }
             |  ){
             |    a
             |  }
             |}
      """.stripMargin,
        project
      )

    Future
      .traverse(0 to 50) { (i) =>
        exec(i)
      }
      .await(seconds = 30)
  }

  "updating single item and relations many times" should "not cause deadlocks" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment).field("a", _.String)
    }
    database.setup(project)

    val createResult = server.query(
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

    def exec(i: Int) =
      server.queryAsync(
        s"""mutation {
             |  updateTodo(
             |    where: { id: "${todoId}" }
             |    data:{
             |      a: "${i}"
             |      comments: {
             |        update: [{where: {id: "${comment1Id}"}, data: {text: "update ${i}"}}]
             |      }
             |    }
             |  ){
             |    a
             |  }
             |}
      """.stripMargin,
        project
      )

    Future
      .traverse(0 to 50) { (i) =>
        exec(i)
      }
      .await(seconds = 30)
  }

  "creating many items with relations" should "not cause deadlocks" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment).field("a", _.String)
    }
    database.setup(project)

    def exec(i: Int) =
      server.queryAsync(
        s"""mutation {
               |  createTodo(
               |    data:{
               |      a: "a",
               |      comments: {
               |        create: [
               |           {text: "first comment: ${i}"}
               |        ]
               |      }
               |    }
               |  ){
               |    a
               |  }
               |}
      """.stripMargin,
        project
      )

    Future.traverse(0 to 50)((i) => exec(i)).await(seconds = 30)
  }

  "deleting many items" should "not cause deadlocks" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment).field("a", _.String)
    }
    database.setup(project)

    def create() =
      server.queryAsync(
        """mutation {
        |  createTodo(
        |    data: {
        |      a: "b",
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

    val todoIds = Future.traverse(0 to 50)((i) => create()).await(seconds = 30).map(_.pathAsString("data.createTodo.id"))

    def exec(id: String) =
      server.queryAsync(
        s"""mutation {
             |  deleteTodo(
             |    where: { id: "${id}" }
             |  ){
             |    a
             |  }
             |}
      """.stripMargin,
        project
      )

    Future.traverse(todoIds)((i) => exec(i)).await(seconds = 30)
  }
}
