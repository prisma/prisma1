package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.gc_values.StringGCValue
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DefaultValueSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project = SchemaDsl() { schema =>
    schema
      .model("ScalarModel")
      .field("optString", _.String)
      .field_!("reqString", _.String, defaultValue = Some(StringGCValue("default")))

  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }

  "A Create Mutation" should "create and return item" in {

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
