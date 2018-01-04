package cool.graph.api.import_export

import cool.graph.api.ApiBaseSpec
import cool.graph.api.database.import_export.BulkImport
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class OptionalBackRelationSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils {

  val project: Project = SchemaDsl() { schema =>
    val model0: SchemaDsl.ModelBuilder = schema
      .model("Model0")
      .field("a", _.String)

    schema
      .model("Model1")
      .field("a", _.String)
      .oneToOneRelation("model0", "doesn't matter", model0, Some("Relation0to1"), includeOtherField = false)

    model0.oneToOneRelation("model0self", "doesn't matter", model0, Some("Relation0to0"), includeOtherField = false)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }
  val importer = new BulkImport(project)

  "Optional back relations" should "be able to be imported" in {

    val nodes = """{"valueType": "nodes", "values": [
                    |{"_typeName": "Model0", "id": "0", "a": "test"},
                    |{"_typeName": "Model1", "id": "1", "a": "test"},
                    |{"_typeName": "Model0", "id": "3", "a": "test"},
                    |{"_typeName": "Model0", "id": "4", "a": "test"},
                    |{"_typeName": "Model0", "id": "5", "a": "test"},
                    |{"_typeName": "Model0", "id": "6", "a": "test"}
                    |]}""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
          |[{"_typeName": "Model0", "id": "0", "fieldName": null},{"_typeName": "Model1", "id": "1", "fieldName": "model0"}],
          |[{"_typeName": "Model0", "id": "3", "fieldName": "model0self"},{"_typeName": "Model0", "id": "4", "fieldName": null}],
          |[{"_typeName": "Model0", "id": "6", "fieldName": null},{"_typeName": "Model0", "id": "5", "fieldName": "model0self"}]
          |]}
          |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(relations).await(5)

    val res0 = server.executeQuerySimple("query{model0s{id, a}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"0","a":"test"},{"id":"3","a":"test"},{"id":"4","a":"test"},{"id":"5","a":"test"},{"id":"6","a":"test"}]}}""")

    val res1 = server.executeQuerySimple("query{model1s{id, a}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"1","a":"test"}]}}""")

    val rel0 = server.executeQuerySimple("query{model0s{id, model0self{id}}}", project).toString
    rel0 should be(
      """{"data":{"model0s":[{"id":"0","model0self":null},{"id":"3","model0self":{"id":"4"}},{"id":"4","model0self":null},{"id":"5","model0self":{"id":"6"}},{"id":"6","model0self":null}]}}""")

    val rel1 = server.executeQuerySimple("query{model1s{id, model0{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"1","model0":{"id":"0"}}]}}""")
  }
}
