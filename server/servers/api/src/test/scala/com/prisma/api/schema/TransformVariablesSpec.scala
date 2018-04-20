package com.prisma.api.schema

import org.scalatest.FlatSpec
import play.api.libs.json._

class TransformVariablesSpec extends FlatSpec {

  "the query" should "work with variables and return no errors if the query is valid" in {
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

    val mutationName  = "CREATED"
    val updatedFields = Set("updated1", "updated2", "updated3")

    def convert(input: JsObject): JsObject = {
      JsObject(input.fields.map {
        case ("updatedFields_contains", JsString(field))                                       => ("boolean", JsBoolean(updatedFields.contains(field)))
        case ("updatedFields_contains_every", JsArray(values))                                 => ("boolean", JsBoolean(values.map(_.as[JsString].value).toSet.subsetOf(updatedFields)))
        case ("updatedFields_contains_some", JsArray(values))                                  => ("boolean", JsBoolean(values.map(_.as[JsString].value).exists(updatedFields.contains)))
        case ("mutation_in", JsString(mutation))                                               => ("boolean", JsBoolean(mutation == mutationName))
        case ("mutation_in", JsArray(values))                                                  => ("boolean", JsBoolean(values.map(_.as[JsString].value).contains(mutationName)))
        case (key: String, obj: JsObject)                                                      => (key, convert(obj))
        case (key: String, JsArray(eles)) if eles.nonEmpty && eles.head.isInstanceOf[JsObject] => (key, JsArray(eles.map(v => convert(v.as[JsObject]))))
        case (key: String, arr: JsArray)                                                       => (key, arr)
        case (key: String, leafJsValue)                                                        => (key, leafJsValue)
      })
    }

    println(mutationName)
    println(variables)
    val conv = convert(variables)
    println(conv)

  }
}
