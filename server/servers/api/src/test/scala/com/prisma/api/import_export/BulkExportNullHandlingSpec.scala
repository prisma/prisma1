package com.prisma.api.import_export

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.DataResolver
import com.prisma.api.import_export.ImportExport.MyJsonProtocol._
import com.prisma.api.import_export.ImportExport.{Cursor, ExportRequest, JsonBundle, ResultFormat}
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

class BulkExportNullHandlingSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  val start       = Cursor(0, 0)
  val emptyResult = ResultFormat(JsonBundle(Vector.empty, 0), Cursor(-1, -1), isFull = false)

  "Exporting nodes" should "be able to handle null in lists or nodes" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Model0 {
        |  id: ID! @id
        |  nonList: String
        |  testList: [String] $scalarListDirective
        |  isNull: String
        |  bla: [Model1]
        |}
        |
        |type Model1 {
        |  id: ID! @id
        |  test: String
        |  isNull: String
        |  bla2: [Model0]
        |}
      """.stripMargin
    }

    database.setup(project)
    database.truncateProjectTables(project)

    server.query("""mutation{createModel0(data: { nonList: "Model0", bla: {create: {test: "Model1"}}}){id}}""", project)

    val exporter                   = new BulkExport(project)
    val dataResolver: DataResolver = this.dataResolver(project)

    val nodeRequest = ExportRequest("nodes", start)
    val nodeResult  = exporter.executeExport(dataResolver, nodeRequest).await(5).as[ResultFormat]
    nodeResult.out.jsonElements.length should be(2)

    val listRequest = ExportRequest("lists", start)
    exporter.executeExport(dataResolver, listRequest).await(5).as[ResultFormat] should be(emptyResult)

    val relationRequest = ExportRequest("relations", start)
    val relationResult  = exporter.executeExport(dataResolver, relationRequest).await(5).as[ResultFormat]
    relationResult.out.jsonElements.length should be(1)
  }

  "Exporting nodes" should "be able to handle null in lists or nodes 2" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
         |type Model0 {
         |  id: ID! @id
         |  test: String
         |  bla1: Model1
         |}
         |
         |type Model1 {
         |  id: ID! @id
         |  test: String
         |  bla1: [Model0] @relation(link: TABLE)
         |}
      """.stripMargin
    }

    database.setup(project)
    database.truncateProjectTables(project)

    server.query("""mutation{createModel0(data: { test: "Model0"}){id}}""", project)
    server.query("""mutation{createModel0(data: { test: "Model0"}){id}}""", project)
    server.query("""mutation{createModel0(data: { test: "Model0"}){id}}""", project)

    val exporter                   = new BulkExport(project)
    val dataResolver: DataResolver = this.dataResolver(project)

    val nodeRequest = ExportRequest("nodes", start)
    val nodeResult  = exporter.executeExport(dataResolver, nodeRequest).await(5).as[ResultFormat]
    nodeResult.out.jsonElements.length should be(3)

    val listRequest = ExportRequest("lists", start)
    exporter.executeExport(dataResolver, listRequest).await(5).as[ResultFormat] should be(emptyResult)

    val relationRequest = ExportRequest("relations", start)
    val relationResult  = exporter.executeExport(dataResolver, relationRequest).await(5).as[ResultFormat]
    relationResult.out.jsonElements.length should be(0)
  }

}
