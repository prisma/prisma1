package com.prisma.api.mutations.embedded

import com.prisma.ConnectorTag
import com.prisma.api.{ApiSpecBase, TestDataModels}
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FlatSpec, Matchers, Retries}

import scala.concurrent.Future

class EmbeddedDeadlockSpec extends FlatSpec with Matchers with Retries with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  import testDependencies.system.dispatcher

  override def withFixture(test: NoArgTest) = {
    val delay = Span(5, Seconds) // we assume that the process gets overwhelmed sometimes by the concurrent requests. Give it a bit of time to recover before retrying.
    withRetry(delay) {
      super.withFixture(test)
    }
  }

  "updating single item many times" should "not cause deadlocks" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo {
        |   id: ID! @id
        |   a: String
        |   comments: [Comment]
        |}
        |
        |type Comment @embedded{
        |   text: String 
        |}
        """
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
        |    comments { text }
        |  }
        |}""",
      project
    )

    val todoId = createResult.pathAsString("data.createTodo.id")

    def exec(i: Int) =
      server.queryAsync(
        s"""mutation {
           |  updateTodo(
           |    where: { id: "$todoId" }
           |    data:{
           |      a: "$i"
           |    }
           |  ){
           |    a
           |  }
           |}
      """,
        project
      )

    Future
      .traverse(0 to 50) { i =>
        exec(i)
      }
      .await(seconds = 30)
  }

  "updating single item many times with scalar list values" should "not cause deadlocks" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo {
        |   id: ID! @id
        |   a: String
        |   tags: [String]
        |   comments: [Comment]
        |}
        |
        |type Comment @embedded {
        |   text: String
        |}
        """
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
        |    comments { text }
        |  }
        |}""",
      project
    )

    val todoId = createResult.pathAsString("data.createTodo.id")

    def exec(i: Int) =
      server.queryAsync(
        s"""mutation {
           |  updateTodo(
           |    where: { id: "$todoId" }
           |    data:{
           |      a: "$i"
           |      tags: {
           |        set: ["important", "doitnow"]
           |      }
           |    }
           |  ){
           |    a
           |  }
           |}
      """,
        project
      )

    Future
      .traverse(0 to 150) { i =>
        exec(i)
      }
      .await(seconds = 30)
  }

  "updating single item and relations many times" should "not cause deadlocks" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo {
        |   id: ID! @id
        |   a: String
        |   tags: [String]
        |   comments: [Comment]
        |}
        |
        |type Comment @embedded {
        |   id: ID! @id
        |   text: String
        |   fieldToUpdate: String
        |}
      """
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
        |}""",
      project
    )

    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")

    def exec(i: Int) =
      server.queryAsync(
        s"""mutation {
           |  updateTodo(
           |    where: { id: "$todoId" }
           |    data:{
           |      a: "$i"
           |      comments: {
           |        update: [{where: {id: "$comment1Id"}, data: {fieldToUpdate: "update $i"}}]
           |      }
           |    }
           |  ){
           |    a
           |  }
           |}
      """,
        project
      )

    Future
      .traverse(0 to 100) { i =>
        exec(i)
      }
      .await(seconds = 30)
  }

  "creating many items with relations" should "not cause deadlocks" in {
    val testDataModels = {
      val dm1 = """
        type Todo {
           id: ID! @id
           a: String
           tags: [String]
           comments: [Comment] @relation(link: INLINE)
        }
        
        type Comment {
           id: ID! @id
           text: String
           todo: Todo
        }
      """

      val dm2 = """
        type Todo {
           id: ID! @id
           a: String
           tags: [String]
           comments: [Comment]
        }
        
        type Comment {
           id: ID! @id
           text: String
           todo: Todo @relation(link: INLINE)
        }
      """

      TestDataModels(mongo = Vector(dm1, dm2), sql = Vector.empty)
    }

    testDataModels.testV11 { project =>
      def exec(i: Int) =
        server.queryAsync(
          s"""mutation {
             |  createTodo(
             |    data:{
             |      a: "a",
             |      comments: {
             |        create: [
             |           {text: "first comment: $i"}
             |        ]
             |      }
             |    }
             |  ){
             |    a
             |  }
             |}
        """,
          project
        )

      Future.traverse(0 to 50)(i => exec(i)).await(seconds = 30)
    }

  }

  "deleting many items" should "not cause deadlocks" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo {
        |   id: ID! @id
        |   a: String
        |   comments: [Comment]
        |}
        |
        |type Comment @embedded{
        |   text: String
        |}
      """
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
          |    comments { text }
          |  }
          |}""",
        project
      )

    val todoIds = Future.traverse(0 to 50)(i => create()).await(seconds = 30).map(_.pathAsString("data.createTodo.id"))

    def exec(id: String) =
      server.queryAsync(
        s"""mutation {
           |  deleteTodo(
           |    where: { id: "$id" }
           |  ){
           |    a
           |  }
           |}
      """,
        project
      )

    Future.traverse(todoIds)(i => exec(i)).await(seconds = 30)
  }
}
