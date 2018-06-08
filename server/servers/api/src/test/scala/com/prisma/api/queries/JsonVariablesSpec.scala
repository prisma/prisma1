package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}


class JsonVariablesSpec extends FlatSpec with Matchers with ApiSpecBase {
  "Empty JSON " should "be validated " in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("MyRequiredJson").field_!("json", _.Json)
    }
    database.setup(project)
    val validJson = """"{\"stuff\": 1, \"nestedStuff\" : {\"stuff\": 2 } }""""
    val invalidJson = """" """"
    val id = server.query(
      s"""mutation {
         | createMyRequiredJson(data: {json: $invalidJson}) {
         |  id
         |  json
         | }
         |}""".stripMargin,
      project).pathAsString("data.createMyRequiredJson.id")

  }
}
