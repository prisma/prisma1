package com.prisma.api.import_export

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.DataResolver
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport.{Cursor, ExportRequest, JsonBundle, ResultFormat}
import com.prisma.deploy.specutils.TestProject
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.models.Project
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

class BulkExportIncompleteSchemaSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  val project: Project = TestProject()

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    database.truncateProjectTables(project)
  }

  val exporter                   = new BulkExport(project)
  val dataResolver: DataResolver = this.dataResolver(project)
  val start                      = Cursor(0, 0)
  val emptyResult                = ResultFormat(JsonBundle(Vector.empty, 0), Cursor(-1, -1), isFull = false)

  "Exporting nodes" should "fail gracefully if no models are defined" in {
    val request = ExportRequest("nodes", start)
    exporter.executeExport(dataResolver, request).await(5).as[ResultFormat] should be(emptyResult)
  }

  "Exporting lists" should "fail gracefully if no lists are defined" in {
    val request = ExportRequest("lists", start)
    exporter.executeExport(dataResolver, request).await(5).as[ResultFormat] should be(emptyResult)
  }

  "Exporting relations" should "fail gracefully if no relations are defined" in {
    val request = ExportRequest("relations", start)
    exporter.executeExport(dataResolver, request).await(5).as[ResultFormat] should be(emptyResult)
  }
}
