package com.prisma.api.import_export

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.DataResolver
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport.{Cursor, ExportRequest, ResultFormat}
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.JsArray

class OptionalBackRelationImportExportSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  val project = SchemaDsl.fromStringV11() {
    """
      |type Model0 {
      |  id: ID! @id
      |  a: String
      |  model0self: Model0 @relation(link: TABLE)
      |}
      |
      |type Model1 {
      |  id: ID! @id
      |  a: String
      |  model0: Model0 @relation(link: TABLE)
      |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  val importer                   = new BulkImport(project)
  val exporter                   = new BulkExport(project)
  val dataResolver: DataResolver = this.dataResolver(project)

  "Relations without back relation" should "be able to be imported if one fieldName is null" in {

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

    val res0 = server.query("query{model0s{id, a}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"0","a":"test"},{"id":"3","a":"test"},{"id":"4","a":"test"},{"id":"5","a":"test"},{"id":"6","a":"test"}]}}""")

    val res1 = server.query("query{model1s{id, a}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"1","a":"test"}]}}""")

    val rel0 = server.query("query{model0s{id, model0self{id}}}", project).toString
    rel0 should be(
      """{"data":{"model0s":[{"id":"0","model0self":null},{"id":"3","model0self":{"id":"4"}},{"id":"4","model0self":null},{"id":"5","model0self":{"id":"6"}},{"id":"6","model0self":null}]}}""")

    val rel1 = server.query("query{model1s{id, model0{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"1","model0":{"id":"0"}}]}}""")
  }

  "Relations without backrelation" should "be able to be imported if the fieldName is missing for one side" in {

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
        |[{"_typeName": "Model0", "id": "0"},{"_typeName": "Model1", "id": "1", "fieldName": "model0"}],
        |[{"_typeName": "Model0", "id": "3", "fieldName": "model0self"},{"_typeName": "Model0", "id": "4"}],
        |[{"_typeName": "Model0", "id": "6"},{"_typeName": "Model0", "id": "5", "fieldName": "model0self"}]
        |]}
        |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(relations).await(5)

    val res0 = server.query("query{model0s{id, a}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"0","a":"test"},{"id":"3","a":"test"},{"id":"4","a":"test"},{"id":"5","a":"test"},{"id":"6","a":"test"}]}}""")

    val res1 = server.query("query{model1s{id, a}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"1","a":"test"}]}}""")

    val rel0 = server.query("query{model0s{id, model0self{id}}}", project).toString
    rel0 should be(
      """{"data":{"model0s":[{"id":"0","model0self":null},{"id":"3","model0self":{"id":"4"}},{"id":"4","model0self":null},{"id":"5","model0self":{"id":"6"}},{"id":"6","model0self":null}]}}""")

    val rel1 = server.query("query{model1s{id, model0{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"1","model0":{"id":"0"}}]}}""")
  }

  "Optional back relations" should "error if no field is provided." in {

    val nodes = """{"valueType": "nodes", "values": [
                  |{"_typeName": "Model0", "id": "0", "a": "test"},
                  |{"_typeName": "Model1", "id": "1", "a": "test"}
                  |]}""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
        |[{"_typeName": "Model0", "id": "0", "fieldName": null},{"_typeName": "Model1", "id": "1", "fieldName": null}]
        |]}
        |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)

    assertThrows[RuntimeException] { importer.executeImport(relations).await(5) }
  }

  "Relations without back relations" should "be able to be exported" in {

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

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("relations", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request).await(5).as[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      """[""" concat
        """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"0"}],""" concat
        """[{"_typeName":"Model0","id":"4"},{"_typeName":"Model0","id":"3","fieldName":"model0self"}],""" concat
        """[{"_typeName":"Model0","id":"6"},{"_typeName":"Model0","id":"5","fieldName":"model0self"}]""" concat "]")
    firstChunk.cursor.table should be(-1)
    firstChunk.cursor.row should be(-1)
  }

}
