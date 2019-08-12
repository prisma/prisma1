package writes

import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FlatSpec, Matchers, Retries}
import util.ConnectorCapability.ScalarListsCapability

import scala.concurrent.Future
import util._

class DeadlockSpec extends FlatSpec with Matchers with Retries with ApiSpecBase with AwaitUtils {

  override def runOnlyForCapabilities = Set(ScalarListsCapability)
  override def doNotRunForConnectors  = Set(ConnectorTag.SQLiteConnectorTag)

  override def withFixture(test: NoArgTest) = {
    val delay = Span(5, Seconds) // we assume that the process gets overwhelmed sometimes by the concurrent requests. Give it a bit of time to recover before retrying.
    withRetry(delay) { super.withFixture(test) }
  }

  "creating many items" should "not cause deadlocks" in {
    val project = ProjectDsl.fromString {
      """
        |model Todo {
        |   id String  @id @default(cuid())
        |   a  String?
        |}"""
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
      """,
          project
        )
      )

    Future.traverse(0 to 50)(i => exec(i)).await(seconds = 30)
  }

  "creating many node with scalar list values" should "not cause deadlocks" in {
    val testDataModels = {
      val dm1 = """
        |model Todo {
        |   id   String   @id @default(cuid())
        |   a    String?
        |   tags String[]
        |}"""

      val dm2 = """
        |model Todo {
        |   id   String  @id @default(cuid())
        |   a    String?
        |   tags String[] // @scalarList(strategy: RELATION)
        |}"""

      TestDataModels(mongo = dm1, sql = dm2)
    }
    testDataModels.testV11 { project =>
      def exec(i: Int) =
        Future(
          server.query(
            s"""mutation {
               |  createTodo(
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
        )

      Future.traverse(0 to 50)(i => exec(i)).await(seconds = 30)
    }

  }
}
