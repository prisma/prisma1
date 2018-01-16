package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.gc_values.{ListGCValue, StringGCValue}
import cool.graph.shared.project_dsl.SchemaDsl
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
