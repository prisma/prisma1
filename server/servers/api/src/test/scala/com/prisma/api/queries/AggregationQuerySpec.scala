package com.prisma.api.queries

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class AggregationQuerySpec extends FlatSpec with Matchers with ApiSpecBase {
  val project = SchemaDsl.fromBuilder { schema =>
    schema.model("Todo").field_!("title", _.String)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.setup(project)
  }

  "the count query" should "return 0" in {
    val result = server.query(
      s"""{
         |  todoesConnection{
         |    aggregate {
         |      count
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.pathAsLong("data.todoesConnection.aggregate.count") should be(0)
  }

  "the count query" should "return 1" in {
    server
      .query(
        s"""mutation {
           |  createTodo(data: {title: "Hello World!"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )

    val result = server.query(
      s"""{
         |  todoesConnection{
         |    aggregate {
         |      count
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.pathAsLong("data.todoesConnection.aggregate.count") should be(1)
  }

  "the count query" should "filter by any field" in {
    val title = "Hello World!"
    server
      .query(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )

    server
      .query(
        s"""{
           |  todoesConnection(where: {title: "INVALID"}){
           |    aggregate {
           |      count
           |    }
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsLong("data.todoesConnection.aggregate.count") should be(0)

    server
      .query(
        s"""{
           |  todoesConnection(where: {title: "$title"}){
           |    aggregate {
           |      count
           |    }
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsLong("data.todoesConnection.aggregate.count") should be(1)
  }

  "the count query" should "obey pagination" taggedAs (IgnoreMongo) in {
    val emptyResult = server.query(
      """{
        |  todoesConnection(first: 3){
        |   aggregate { count }
        |  }
        |}
      """.stripMargin,
      project
    )
    emptyResult should equal("""{"data":{"todoesConnection":{"aggregate":{"count":0}}}}""".parseJson)

    createTodo("1")
    createTodo("2")
    createTodo("3")
    createTodo("4")

    val result = server.query(
      """{
        |  todoesConnection(first: 3){
        |   aggregate { count }
        |  }
        |}
      """.stripMargin,
      project
    )
    result should equal("""{"data":{"todoesConnection":{"aggregate":{"count":3}}}}""".parseJson)

    val result2 = server.query(
      """{
        |  todoesConnection(first: 5){
        |   aggregate { count }
        |  }
        |}
      """.stripMargin,
      project
    )
    result2 should equal("""{"data":{"todoesConnection":{"aggregate":{"count":4}}}}""".parseJson)
  }

  def createTodo(title: String) = {
    server.query(
      s"""mutation {
         |  createTodo(data: {title: "$title"}) {
         |    id
         |  }
         |}""".stripMargin,
      project
    )
  }
}
