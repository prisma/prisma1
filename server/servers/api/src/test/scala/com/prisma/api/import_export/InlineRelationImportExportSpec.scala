package com.prisma.api.import_export

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.DataResolver
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport.{Cursor, ExportRequest, ResultFormat}
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.JsArray

class InlineRelationImportExportSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  "Relations with inline manifestations" should "be able to be imported" in {

    lazy val project = SchemaDsl.fromStringV11() {
      """
      |type Model0 {
      |  id: ID! @id
      |  a: String
      |  model1: Model1 @relation(link: INLINE)
      |}
      |
      |type Model1 {
      |  id: ID! @id
      |  a: String
      |  model0: Model0
      |}
    """
    }
    database.setup(project)
    lazy val importer                   = new BulkImport(project)
    lazy val exporter                   = new BulkExport(project)
    lazy val dataResolver: DataResolver = this.dataResolver(project)

    val nodes = """{"valueType": "nodes", "values": [
                    |{"_typeName": "Model0", "id": "1", "a": "test"},
                    |{"_typeName": "Model0", "id": "2", "a": "test"},
                    |{"_typeName": "Model0", "id": "3", "a": "test"},
                    |{"_typeName": "Model1", "id": "4", "a": "test"},
                    |{"_typeName": "Model1", "id": "5", "a": "test"},
                    |{"_typeName": "Model1", "id": "6", "a": "test"}
                    |]}""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
          |[{"_typeName": "Model0", "id": "1", "fieldName": "model1"},{"_typeName": "Model1", "id": "4", "fieldName": "model0"}],
          |[{"_typeName": "Model0", "id": "2", "fieldName": "model1"},{"_typeName": "Model1", "id": "5", "fieldName": "model0"}],
          |[{"_typeName": "Model0", "id": "3", "fieldName": "model1"},{"_typeName": "Model1", "id": "6", "fieldName": "model0"}]
          |]}
          |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(relations).await(5)

    val res0 = server.query("query{model0s{id, a}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"1","a":"test"},{"id":"2","a":"test"},{"id":"3","a":"test"}]}}""")

    val res1 = server.query("query{model1s{id, a}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"4","a":"test"},{"id":"5","a":"test"},{"id":"6","a":"test"}]}}""")

    val rel0 = server.query("query{model0s{id, model1{id}}}", project).toString
    rel0 should be("""{"data":{"model0s":[{"id":"1","model1":{"id":"4"}},{"id":"2","model1":{"id":"5"}},{"id":"3","model1":{"id":"6"}}]}}""")

    val rel1 = server.query("query{model1s{id, model0{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"4","model0":{"id":"1"}},{"id":"5","model0":{"id":"2"}},{"id":"6","model0":{"id":"3"}}]}}""")

    val res = exporter.executeExport(dataResolver, ExportRequest("relations", Cursor(0, 0))).await

    res.toString should be(
      """{"out":{"jsonElements":[[{"_typeName":"Model1","id":"4","fieldName":"model0"},{"_typeName":"Model0","id":"1","fieldName":"model1"}],[{"_typeName":"Model1","id":"5","fieldName":"model0"},{"_typeName":"Model0","id":"2","fieldName":"model1"}],[{"_typeName":"Model1","id":"6","fieldName":"model0"},{"_typeName":"Model0","id":"3","fieldName":"model1"}]],"size":321},"cursor":{"table":-1,"row":-1,"field":-1,"array":-1},"isFull":false}""")
  }

  "Relations with inline manifestations" should "be able to be imported 2" in {

    lazy val project = SchemaDsl.fromStringV11() {
      """
        |type Model0 {
        |  id: ID! @id
        |  a: String
        |  model1: Model1 
        |}
        |
        |type Model1 {
        |  id: ID! @id
        |  a: String
        |  model0: Model0 @relation(link: INLINE)
        |}
      """
    }
    database.setup(project)
    lazy val importer                   = new BulkImport(project)
    lazy val exporter                   = new BulkExport(project)
    lazy val dataResolver: DataResolver = this.dataResolver(project)

    val nodes = """{"valueType": "nodes", "values": [
                  |{"_typeName": "Model0", "id": "1", "a": "test"},
                  |{"_typeName": "Model0", "id": "2", "a": "test"},
                  |{"_typeName": "Model0", "id": "3", "a": "test"},
                  |{"_typeName": "Model1", "id": "4", "a": "test"},
                  |{"_typeName": "Model1", "id": "5", "a": "test"},
                  |{"_typeName": "Model1", "id": "6", "a": "test"}
                  |]}""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
        |[{"_typeName": "Model0", "id": "1", "fieldName": "model1"},{"_typeName": "Model1", "id": "4", "fieldName": "model0"}],
        |[{"_typeName": "Model0", "id": "2", "fieldName": "model1"},{"_typeName": "Model1", "id": "5", "fieldName": "model0"}],
        |[{"_typeName": "Model0", "id": "3", "fieldName": "model1"},{"_typeName": "Model1", "id": "6", "fieldName": "model0"}]
        |]}
        |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(relations).await(5)

    val res0 = server.query("query{model0s{id, a}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"1","a":"test"},{"id":"2","a":"test"},{"id":"3","a":"test"}]}}""")

    val res1 = server.query("query{model1s{id, a}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"4","a":"test"},{"id":"5","a":"test"},{"id":"6","a":"test"}]}}""")

    val rel0 = server.query("query{model0s{id, model1{id}}}", project).toString
    rel0 should be("""{"data":{"model0s":[{"id":"1","model1":{"id":"4"}},{"id":"2","model1":{"id":"5"}},{"id":"3","model1":{"id":"6"}}]}}""")

    val rel1 = server.query("query{model1s{id, model0{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"4","model0":{"id":"1"}},{"id":"5","model0":{"id":"2"}},{"id":"6","model0":{"id":"3"}}]}}""")
  }

}
