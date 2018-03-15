package com.prisma.api.import_export

import com.prisma.api.ApiBaseSpec
import com.prisma.api.connector.mysql.database.DataResolver
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport.{Cursor, ExportRequest, ResultFormat}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class SingleValueImportExportSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils {

  val project: Project = SchemaDsl() { schema =>
    val enum = schema.enum("Enum", Vector("HA", "HO"))

    schema
      .model("Model0")
      .field("string", _.String)
      .field("int", _.Int)
      .field("float", _.Float)
      .field("boolean", _.Boolean)
      .field("datetime", _.DateTime)
      .field("enum", _.Enum, enum = Some(enum))
      .field("json", _.Json)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }
  val importer                   = new BulkImport(project)
  val exporter                   = new BulkExport(project)
  val dataResolver: DataResolver = this.dataResolver(project)

  "Exporting nodes" should "work (with filesize limit set to 1000 for test)" in {

    val nodes =
      """{ "valueType": "nodes", "values": [
        |{"_typeName": "Model0", "id": "0","string": "string", "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "1","int": 1, "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "2","float": 1.2345, "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "3","boolean": true, "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "4","datetime": "2018-01-07T15:55:19Z", "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "5","enum": "HA", "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "6","json": {"a":2}, "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "7","json": [{"a": {"b": {"c": [1,2,3]}}},{"a": {"b": {"c": [1,2,3]}}}], "createdAt":"2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await(5).toString should be("[]")

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("nodes", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    val res = JsArray(firstChunk.out.jsonElements).toString

    res should include("""{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","string":"string","id":"0","createdAt":"2017-11-29T14:35:13.000Z"}""")
    res should include("""{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","float":1.2345,"id":"2","createdAt":"2017-11-29T14:35:13.000Z"}""")
    res should include("""{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","id":"3","boolean":true,"createdAt":"2017-11-29T14:35:13.000Z"}""")
    res should include(
      """{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","datetime":"2018-01-07T15:55:19.000Z","id":"4","createdAt":"2017-11-29T14:35:13.000Z"}""")
    res should include("""{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","id":"5","enum":"HA","createdAt":"2017-11-29T14:35:13.000Z"}""")
    res should include("""{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","json":{"a":2},"id":"6","createdAt":"2017-11-29T14:35:13.000Z"}""")

    firstChunk.cursor.table should be(0)
    firstChunk.cursor.row should be(7)

    val request2    = ExportRequest("nodes", firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2.toJson).await(5).convertTo[ResultFormat]

    val res2 = JsArray(secondChunk.out.jsonElements).toString

    res2 should include(
      """[{"updatedAt":"2017-12-05T12:34:23.000Z","_typeName":"Model0","json":[{"a":{"b":{"c":[1,2,3]}}},{"a":{"b":{"c":[1,2,3]}}}],"id":"7","createdAt":"2017-11-29T14:35:13.000Z"}]""")

    secondChunk.cursor.table should be(-1)
    secondChunk.cursor.row should be(-1)

  }
}
