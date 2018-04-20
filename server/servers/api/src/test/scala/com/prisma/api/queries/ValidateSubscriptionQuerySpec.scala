package com.prisma.api.queries

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

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

  "the query" should "work with variables and return no errors if the query is valid" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    database.setup(project)

    val query = """
                  |subscription ($_where: TodoSubscriptionWhereInput!){
                  |  todo(where: $_where) {
                  |    node {
                  |      id
                  |    }
                  |  }
                  |}
                """.stripMargin

    val variables = Json.parse("""{
                                 |  "_where": {
                                 |    "mutation_in": [
                                 |      "CREATED",
                                 |      "UPDATED"
                                 |    ]
                                 |  }
                                 |}""".stripMargin).asJsObject

    val result = server.queryPrivateSchema(
      s"""{
         |  validateSubscriptionQuery(query: ${escapeString(query)}){
         |    errors
         |  }
         |}""".stripMargin,
      project,
      variables
    )

    result.pathAsSeq("data.validateSubscriptionQuery.errors") should have(size(0))
  }

}
