package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.gc_values.{ListGCValue, StringGCValue}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DefaultValueSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "A Create Mutation on a non-list field" should "utilize the defaultValue" in {
    val project = SchemaDsl() { schema =>
      schema.model("ScalarModel").field_!("reqString", _.String, defaultValue = Some(StringGCValue("default")))
    }
    database.setup(project)

    val res = server.executeQuerySimple(
      s"""mutation {
         |  createScalarModel(data: {
         |    }
         |  ){
         |  reqString
         |  }
         |}""".stripMargin,
      project = project
    )

    res.toString should be(s"""{"data":{"createScalarModel":{"reqString":"default"}}}""")

    val queryRes = server.executeQuerySimple("""{ scalarModels{reqString}}""", project = project)

    queryRes.toString should be(s"""{"data":{"scalarModels":[{"reqString":"default"}]}}""")
  }
}
