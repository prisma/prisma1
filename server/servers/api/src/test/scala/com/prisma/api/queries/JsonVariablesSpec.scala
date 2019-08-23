package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.Prisma2Capability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._

class JsonVariablesSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set(Prisma2Capability)

  val project = SchemaDsl.fromStringV11() {
    """
      | type MyRequiredJson {
      |   id: ID! @id
      |   json: Json!
      | }
    """.stripMargin
  }
  database.setup(project)

  val queryString =
    """mutation createMyRequiredJson($json: Json!) {
      | createMyRequiredJson(data: {json: $json}) {
      |  id
      |  json
      | }
      |}"""

  "Invalid Json" should "be rejected" in {
    val variables = Json.parse("""{"json": " "}""")

    server.queryThatMustFail(queryString, project, variables = variables, errorCode = 0, errorContains = """Reason: Not valid JSON""")
  }

  "Invalid json 2" should "be rejected" in {
    val variables = Json.parse("""{"json":  1 }""")

    server.queryThatMustFail(queryString, project, variables = variables, errorCode = 0, errorContains = """Reason: Not valid JSON""")
  }

  "Valid Json object" should "be validated " in {
    val variables = Json.parse("""{"json": {"test": 1}}""")

    server.query(queryString, project, variables = variables)
  }

  "Valid Json array" should "be validated " in {
    val variables = Json.parse("""{"json": []}""")
    server.query(queryString, project, variables = variables)
  }

}
