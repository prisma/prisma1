package cool.graph.api.queries

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class AggregationQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {
  "the count query" should "return 0" in {

    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val result = server.executeQuerySimple(
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
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    server
      .executeQuerySimple(
        s"""mutation {
           |  createTodo(data: {title: "Hello World!"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )

    val result = server.executeQuerySimple(
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
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val title = "Hello World!"
    server
      .executeQuerySimple(
        s"""mutation {
           |  createTodo(data: {title: "$title"}) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )

    server
      .executeQuerySimple(
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
      .executeQuerySimple(
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
}
