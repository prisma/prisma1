package com.prisma.api.mutations

import java.util.concurrent.Executors

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContext, Future}

class DeadlockSpec extends FlatSpec with Matchers with ApiBaseSpec {

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(100))

  "creating many items" should "not cause deadlocks" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment).field("a", _.String)
    }
    database.setup(project)

    implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(100))

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

    Await.result(Future.traverse(0 to 50)((i) => exec(i)), Duration.Inf)
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
      Future(
        server.query(
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
      )

    Await.result(Future.traverse(0 to 50)((i) => exec(i)), Duration.Inf)
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
      Future(
        server.query(
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
      )

    Await.result(Future.traverse(0 to 50)((i) => exec(i)), Duration.Inf)
  }

  "creating many items with relations" should "not cause deadlocks" in {
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
      )

    Await.result(Future.traverse(0 to 50)((i) => exec(i)), Duration.Inf)
  }

}
