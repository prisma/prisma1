package cool.graph.api.queries

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class ValidateSubscriptionQuerySpec extends FlatSpec with Matchers with ApiBaseSpec {
  "the query" should "return errors if the query is invalid GraphQL" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val query = "broken query"
    val result = server.queryPrivateSchema(
      s"""{
         |  validateSubscriptionQuery(query: ${escapeString(query)}){
         |    errors
         |  }
         |}""".stripMargin,
      project
    )

    result.pathAsSeq("data.validateSubscriptionQuery.errors") should have(size(1))
  }

  "the query" should "return errors if the query contains unknown models" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val query = """
                  |subscription {
                  |  unknownModel(where: {mutation_in: UPDATED}) {
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
         |  validateSubscriptionQuery(query: ${escapeString(query)}){
         |    errors
         |  }
         |}""".stripMargin,
      project
    )

    result.pathAsSeq("data.validateSubscriptionQuery.errors") should have(size(1))
  }

  "the query" should "return no errors if the query is valid" in {
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
         |  validateSubscriptionQuery(query: ${escapeString(query)}){
         |    errors
         |  }
         |}""".stripMargin,
      project
    )

    result.pathAsSeq("data.validateSubscriptionQuery.errors") should have(size(0))
  }
}
