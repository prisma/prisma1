package cool.graph.api.import_export

import cool.graph.api.ApiBaseSpec
import cool.graph.api.database.DataResolver
import cool.graph.api.database.import_export.BulkExport
import cool.graph.api.database.import_export.ImportExport.MyJsonProtocol._
import cool.graph.api.database.import_export.ImportExport.{Cursor, ExportRequest, JsonBundle, ResultFormat}
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import spray.json._

class BulkExportIncompleteSchemaSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils {

  val project: Project = SchemaDsl() { schema =>
    }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }

  val exporter                   = new BulkExport(project)
  val dataResolver: DataResolver = this.dataResolver(project)

  "Exporting nodes" should "fail gracefully if no models are defined" in {

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("nodes", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    firstChunk should be(ResultFormat(JsonBundle(Vector.empty, 0), Cursor(-1, -1, -1, -1), isFull = false))
  }

  "Exporting lists" should "fail gracefully if no relations are defined" in {

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("lists", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    firstChunk should be(ResultFormat(JsonBundle(Vector.empty, 0), Cursor(-1, -1, -1, -1), isFull = false))
  }

  "Exporting relations" should "fail gracefully if no listfields are defined" in {

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("relations", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    firstChunk should be(ResultFormat(JsonBundle(Vector.empty, 0), Cursor(-1, -1, -1, -1), isFull = false))
  }
}
