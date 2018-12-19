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

class ImportJsonFormatSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  "Import json nodes" should "work" in {
    val project: Project = SchemaDsl.fromBuilder { schema =>
      val model1 = schema
        .model("Model0")
        .field("a", _.String)
        .field("b", _.Json)
        .field("updatedAt", _.DateTime)
        .field("createdAt", _.DateTime)
    }

    database.setup(project)
    database.truncateProjectTables(project)
    val dataResolver: DataResolver = this.dataResolver(project)

    val nodes =
      ("""{ "valueType": "nodes", "values": [""" +
        """{"_typeName": "Model0", "id": "0","a": "test1", "createdAt": "2017-12-05T12:34:23.000Z", "updatedAt": "2017-12-05T12:34:23.000Z"},""" +
        """{"_typeName": "Model0", "id": "1","a": "test1", "b": {}, "createdAt": "2017-12-05T12:34:23.000Z", "updatedAt": "2017-12-05T12:34:23.000Z"},""" +
        """{"_typeName": "Model0", "id": "2","a": "test2", "b": {"a": "b"}, "createdAt": "2017-12-05T12:34:23.000Z", "updatedAt": "2017-12-05T12:34:23.000Z"},""" +
        """{"_typeName": "Model0", "id": "3","a": "test2", "b": {"a": 2}, "createdAt": "2017-12-05T12:34:23.000Z", "updatedAt": "2017-12-05T12:34:23.000Z"}"""
        + """]}""").parseJson

    val importer = new BulkImport(project)
    val exporter = new BulkExport(project)
    importer.executeImport(nodes).await(5).toString should be("[]")

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("nodes", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request).await(5).as[ResultFormat]

    println(firstChunk)

    JsArray(firstChunk.out.jsonElements).toString should be(
      "[" +
        """{"_typeName":"Model0","id":"0","updatedAt":"2017-12-05T12:34:23.000Z","a":"test1","createdAt":"2017-12-05T12:34:23.000Z"},""" +
        """{"_typeName":"Model0","id":"1","updatedAt":"2017-12-05T12:34:23.000Z","a":"test1","b":{},"createdAt":"2017-12-05T12:34:23.000Z"},""" +
        """{"_typeName":"Model0","id":"2","updatedAt":"2017-12-05T12:34:23.000Z","a":"test2","b":{"a":"b"},"createdAt":"2017-12-05T12:34:23.000Z"},""" +
        """{"_typeName":"Model0","id":"3","updatedAt":"2017-12-05T12:34:23.000Z","a":"test2","b":{"a":2},"createdAt":"2017-12-05T12:34:23.000Z"}""" + "]")
  }
}
