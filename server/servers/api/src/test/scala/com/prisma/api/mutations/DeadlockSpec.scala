package com.prisma.api.mutations

import com.prisma.ConnectorTag
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.ScalarListsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.time.{Seconds, Span}
import org.scalatest.{FlatSpec, Matchers, Retries}

import scala.concurrent.Future

class DeadlockSpec extends FlatSpec with Matchers with Retries with ApiSpecBase with AwaitUtils {
  override def doNotRunForPrototypes: Boolean = true
  override def runOnlyForCapabilities         = Set(ScalarListsCapability)
  override def doNotRunForConnectors          = Set(ConnectorTag.SQLiteConnectorTag)

  import testDependencies.system.dispatcher

  override def withFixture(test: NoArgTest) = {
    val delay = Span(5, Seconds) // we assume that the process gets overwhelmed sometimes by the concurrent requests. Give it a bit of time to recover before retrying.
    withRetry(delay) { super.withFixture(test) }
  }

  "creating many items" should "not cause deadlocks" in {
    val project = SchemaDsl.fromString() {
      """
        |type Todo {
        |   id: ID! @unique
        |   a: String
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
    val project = SchemaDsl.fromString() {
      """
        |type Todo {
        |   id: ID! @unique
        |   a: String
        |   tags: [String]
        |}"""
    }
    database.setup(project)

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
