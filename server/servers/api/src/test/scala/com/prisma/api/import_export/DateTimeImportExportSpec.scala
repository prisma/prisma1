package com.prisma.api.import_export

import java.util.Date

import com.prisma.api.ApiBaseSpec
import com.prisma.api.connector.DataResolver
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport.{Cursor, ExportRequest, ResultFormat}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json.JsArray
import spray.json._

class DateTimeImportExportSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils {

  val project: Project = SchemaDsl() { schema =>
    schema
      .model("Model3")
      .field("createdAt", _.DateTime)
      .field("updatedAt", _.DateTime)
      .field("a", _.String)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }
  val importer = new BulkImport(project)

  "DateTimeFormat" should "work" in {
    val now             = new Date()
    val dateTimeFormat  = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").withZoneUTC()
    val dateTimeFormat2 = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss").withZoneUTC()

    val utc                        = new DateTime(now).withZone(DateTimeZone.UTC)
    val utcString                  = dateTimeFormat.print(utc)
    val utcStringOutput            = dateTimeFormat2.print(utc) + ".000Z"
    val utcStringOutputOneSecLater = dateTimeFormat2.print(utc.plusSeconds(1)) + ".000Z"

    val nodes = s"""{"valueType": "nodes", "values": [{"_typeName": "Model3", "id": "0", "a": "test", "updatedAt": "$utcString"}]}""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)

    val res = server.query("query{model3s{createdAt, updatedAt}}", project).toString

    val result = res match {
      case exact if exact == s"""{"data":{"model3s":[{"createdAt":"$utcStringOutput","updatedAt":"$utcStringOutput"}]}}"""                => true
      case delayed if delayed == s"""{"data":{"model3s":[{"createdAt":"$utcStringOutputOneSecLater","updatedAt":"$utcStringOutput"}]}}""" => true
      case _                                                                                                                              => false
    }

    result should be(true)
  }

  "Exporting nodes" should "produce the correct ISO 8601 DateTime Format" in {
    val project: Project = SchemaDsl() { schema =>
      val model1 = schema
        .model("Model0")
        .field("a", _.String)
        .field("b", _.Int)
        .field("createdAt", _.DateTime)
        .field("updatedAt", _.DateTime)
    }

    database.setup(project)
    database.truncate(project)
    val dataResolver: DataResolver = this.dataResolver(project)

    val nodes =
      """{ "valueType": "nodes", "values": [{"_typeName": "Model0", "id": "0","a": "test1", "b": 0, "createdAt": "2017-12-05T12:34:23.000Z", "updatedAt": "2017-12-05T12:34:23.000Z"}]}""".parseJson

    val importer = new BulkImport(project)
    val exporter = new BulkExport(project)
    importer.executeImport(nodes).await(5).toString should be("[]")

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("nodes", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request).await(5).as[ResultFormat]
    println(firstChunk)

    JsArray(firstChunk.out.jsonElements).toString should be(
      """[{"_typeName":"Model0","id":"0","a":"test1","b":0,"createdAt":"2017-12-05T12:34:23.000Z","updatedAt":"2017-12-05T12:34:23.000Z"}]""")
  }
}
