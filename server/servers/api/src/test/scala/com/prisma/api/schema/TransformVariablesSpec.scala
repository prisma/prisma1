package com.prisma.api.schema

import com.prisma.subscriptions.schema.VariablesTransformer
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

class TransformVariablesSpec extends FlatSpec with Matchers {

  "Created" should "work with variables and return no errors if the query is valid" in {
    val variables: JsObject = Json.parse("""{
        |  "_where": {
        |       "AND":[
        |    {"mutation_in": ["CREATED"]},
        |    {"mutation_in": "UPDATED"},
        |    {"field": "value"},
        |    {"listField": [1,2,3,4]},
        |    {"booleans": true},
        |    {"float": 1.2323}
        |    ]
        |  }
        |}""".stripMargin).as[JsObject]

    val mutationName  = com.prisma.shared.models.ModelMutationType.Created
    val updatedFields = Set.empty[String]

    val (matches, newVariables) = VariablesTransformer.evaluateInMemoryFilters(variables, mutationName, updatedFields)

    matches should be(false)

    println(newVariables)

    newVariables.toString should be("""{"_where":{"AND":[{"field":"value"},{"listField":[1,2,3,4]},{"booleans":true},{"float":1.2323}]}}""")
  }

  "Created with array" should "work with variables and return no errors if the query is valid" in {
    val variables: JsObject = Json.parse("""{
                                           |  "_where": {
                                           |       "AND":[
                                           |    {"mutation_in": ["CREATED", "UPDATED", "DELETED"]},
                                           |    {"mutation_in": ["UPDATED", "DELETED"]},
                                           |    {"mutation_in": "UPDATED"},
                                           |    {"field": "value"},
                                           |    {"listField": [1,2,3,4]},
                                           |    {"booleans": true},
                                           |    {"float": 1.2323}
                                           |    ]
                                           |  }
                                           |}""".stripMargin).as[JsObject]

    val mutationName  = com.prisma.shared.models.ModelMutationType.Created
    val updatedFields = Set.empty[String]

    val (matches, newVariables) = VariablesTransformer.evaluateInMemoryFilters(variables, mutationName, updatedFields)
    matches should be(false)
    newVariables.toString should be("""{"_where":{"AND":[{"field":"value"},{"listField":[1,2,3,4]},{"booleans":true},{"float":1.2323}]}}""")
  }

  "Updated with some updated fields" should "work with variables and return no errors if the query is valid" in {
    val variables: JsObject = Json.parse("""{
                                           |  "_where": {
                                           |       "AND":[
                                           |    {"mutation_in": ["CREATED"]},
                                           |    {"mutation_in": "UPDATED"},
                                           |    {"field": "value"},
                                           |    {"listField": [1,2,3,4]},
                                           |    {"booleans": true},
                                           |    {"OR":[
                                           |         {"updatedFields_contains_every": ["updated1"]},
                                           |         {"updatedFields_contains_every": ["updated1", "updated5"]},
                                           |         {"updatedFields_contains_some": ["updated1"]},
                                           |         {"updatedFields_contains_some": ["updated5"]},
                                           |         {"updatedFields_contains": "updated1"},
                                           |         {"updatedFields_contains": "updated5"}
                                           |       ]},
                                           |    {"float": 1.2323}
                                           |    ]
                                           |  }
                                           |}""".stripMargin).as[JsObject]

    val mutationName  = com.prisma.shared.models.ModelMutationType.Updated
    val updatedFields = Set("updated1", "updated2", "updated3")

    val (matches, newVariables) = VariablesTransformer.evaluateInMemoryFilters(variables, mutationName, updatedFields)
    matches should be(false)
    newVariables.toString should be("""{"_where":{"AND":[{"field":"value"},{"listField":[1,2,3,4]},{"booleans":true},{"OR":[]},{"float":1.2323}]}}""")

  }
}
