package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

class JsonVariablesSpec extends FlatSpec with Matchers with ApiSpecBase {
  "Empty JSON " should "be validated " in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("MyRequiredJson").field_!("json", _.Json)
    }
    database.setup(project)
    val queryString =
      """mutation createMyRequiredJson($json: Json!) {
        | createMyRequiredJson(data: {json: $json}) {
        |  id
        |  json
        | }
        |}""".stripMargin
    val variables = Json.parse(
      s"""{
        |   "json": " "
        |  }""".stripMargin)
    val id = server.query(queryString, project, "", variables).pathAsString("data.createMyRequiredJson.id")

  }
}
