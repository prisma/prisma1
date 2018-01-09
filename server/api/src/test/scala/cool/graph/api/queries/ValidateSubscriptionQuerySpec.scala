package cool.graph.api.queries

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class ValidateSubscriptionQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {
  "the query" should "return false if the query is not valid" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val query = "broken query"
    val result = server.queryPrivateSchema(
      s"""{
         |  validateSubscriptionQuery(query: ${escapeString(query)})
         |}""".stripMargin,
      project
    )

    result.pathAsBool("data.validateSubscriptionQuery") should be(false)
  }

  "the query" should "return true if the query is valid" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val query = """
        |subscription {
        |  todo(where: {mutation_in: UPDATED}) {
        |    mutation
        |    previousValues {
        |      id
        |      title
        |    }
        |  }
        |}
      """.stripMargin
    val result = server.queryPrivateSchema(
      s"""{
         |  validateSubscriptionQuery(query: ${escapeString(query)})
         |}""".stripMargin,
      project
    )

    result.pathAsBool("data.validateSubscriptionQuery") should be(true)
  }
}
