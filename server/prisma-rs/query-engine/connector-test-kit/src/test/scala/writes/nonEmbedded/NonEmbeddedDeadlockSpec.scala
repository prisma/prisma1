package writes.nonEmbedded

import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FlatSpec, Matchers, Retries}
import util.ConnectorCapability.{JoinRelationLinksCapability, NonEmbeddedScalarListCapability, ScalarListsCapability}
import util._

import scala.concurrent.Future

class NonEmbeddedDeadlockSpec extends FlatSpec with Matchers with Retries with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability, ScalarListsCapability)
  override def doNotRunForConnectors  = Set(ConnectorTag.SQLiteConnectorTag)

  override def withFixture(test: NoArgTest) = {
    val delay = Span(5, Seconds) // we assume that the process gets overwhelmed sometimes by the concurrent requests. Give it a bit of time to recover before retrying.
    withRetry(delay) {
      super.withFixture(test)
    }
  }

  "updating single item many times" should "not cause deadlocks" in {
    testDataModels.testV11 { project =>
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
  }

  "updating single item many times with scalar list values" should "not cause deadlocks" in {
    val scalarListStrategy = if (capabilities.has(NonEmbeddedScalarListCapability)) {
      "@scalarList(strategy: RELATION)"
    } else {
      ""
    }
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Todo {
        |   id: ID! @id
        |   a: String
        |   tags: [String] $scalarListStrategy
        |}
        """
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createTodo(
        |    data: {}
        |  ){
        |    id
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
    testDataModels.testV11 { project =>
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
      val comment2Id = createResult.pathAsString("data.createTodo.comments.[1].id")

      def exec(i: Int) =
        server.queryAsync(
          s"""mutation {
           |  updateTodo(
           |    where: { id: "$todoId" }
           |    data:{
           |      a: "$i"
           |      comments: {
           |        update: [{where: {id: "$comment1Id"}, data: {text: "update $i"}}]
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
  }

  "creating many items with relations" should "not cause deadlocks" in {
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
    testDataModels.testV11 { project =>
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

  val testDataModels = {
    val dm1 = """
        type Todo {
           id: ID! @id
           a: String
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
           comments: [Comment]
        }
        
        type Comment {
           id: ID! @id
           text: String
           todo: Todo @relation(link: INLINE)
        }
      """

    val dm3 = """
        type Todo {
           id: ID! @id
           a: String
           comments: [Comment]
        }
        
        type Comment {
           id: ID! @id
           text: String
           todo: Todo
        }
      """

    val dm4 = """
        type Todo {
           id: ID! @id
           a: String
           comments: [Comment] @relation(link: TABLE)
        }
        
        type Comment {
           id: ID! @id
           text: String
           todo: Todo
        }
      """

    TestDataModels(mongo = Vector(dm1, dm2), sql = Vector(dm3, dm4))
  }
}
