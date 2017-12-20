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

class ExportNullHandlingSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils {

  val project: Project = SchemaDsl() { schema =>
    val model1 = schema
      .model("Model1")
      .field("test", _.String)
      .field("isNull", _.String)

    val model0 = schema
      .model("Model0")
      .manyToManyRelation("bla", "bla", model1)
      .field("nonList", _.String)
      .field("testList", _.String, isList = true)
      .field("isNull", _.String)
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
  val start                      = Cursor(0, 0, 0, 0)
  val emptyResult                = ResultFormat(JsonBundle(Vector.empty, 0), Cursor(-1, -1, -1, -1), isFull = false)

  "Exporting nodes" should "be able to handle null in lists or nodes" in {

    server.executeQuerySimple("""mutation{createModel0(data: { nonList: "Model0", bla: {create: {test: "Model1"}}}){id}}""", project)

    val nodeRequest = ExportRequest("nodes", start)
    val nodeResult  = exporter.executeExport(dataResolver, nodeRequest.toJson).await(5).convertTo[ResultFormat]
    nodeResult.out.jsonElements.length should be(2)

    val listRequest = ExportRequest("lists", start)
    exporter.executeExport(dataResolver, listRequest.toJson).await(5).convertTo[ResultFormat] should be(emptyResult)

    val relationRequest = ExportRequest("relations", start)
    val relationResult  = exporter.executeExport(dataResolver, relationRequest.toJson).await(5).convertTo[ResultFormat]
    relationResult.out.jsonElements.length should be(1)
  }
}
