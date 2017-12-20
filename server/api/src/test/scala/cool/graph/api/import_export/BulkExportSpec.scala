package cool.graph.api.import_export

import cool.graph.api.ApiBaseSpec
import cool.graph.api.database.DataResolver
import cool.graph.api.database.import_export.ImportExport.{Cursor, ExportRequest, ResultFormat}
import cool.graph.api.database.import_export.{BulkExport, BulkImport}
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}
import spray.json._
import cool.graph.api.database.import_export.ImportExport.MyJsonProtocol._

class BulkExportSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils {

  val project = SchemaDsl() { schema =>
    val model0: SchemaDsl.ModelBuilder = schema
      .model("Model0")
      .field("a", _.String)
      .field("b", _.Int)

    val model1: SchemaDsl.ModelBuilder = schema
      .model("Model1")
      .field("a", _.String)
      .field("b", _.Int)
      .field("listField", _.Int, isList = true)

    val model2: SchemaDsl.ModelBuilder = schema
      .model("Model2")
      .field("a", _.String)
      .field("b", _.Int)
      .field("name", _.String)

    model0.manyToManyRelation("relation0top", "relation0bottom", model0, Some("Relation0"))
    model0.manyToManyRelation("model1", "model0", model1, Some("Relation1"))
    model2.manyToManyRelation("model1", "model2", model1, Some("Relation2"))
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
        |{"_typeName": "Model0", "id": "0","a": "test1", "b": 0, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model1", "id": "1","a": "test2", "b": 1, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model2", "id": "2", "a": "test3", "b": 2, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "3", "a": "test4", "b": 3, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "4", "a": "test1", "b": 0, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model1", "id": "5", "a": "test2", "b": 1, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model2", "id": "6", "a": "test3", "b": 2, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "7", "a": "test4", "b": 3, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "8", "a": "test1", "b": 0, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model1", "id": "9", "a": "test2", "b": 1, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model2", "id": "10", "a": "test3", "b": 2, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "11", "a": "test4", "b": 3, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model2", "id": "12", "a": "test3", "b": 2, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "13", "a": "test4", "b": 3, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "14", "a": "test1", "b": 0, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model1", "id": "15", "a": "test2", "b": 1, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model2", "id": "16", "a": "test3", "b": 2, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"},
        |{"_typeName": "Model0", "id": "17", "a": "test4", "b": 3, "createdAt": "2017-11-29 14:35:13", "updatedAt":"2017-12-05 12:34:23.0"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await(5).toString should be("[]")

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("nodes", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      "[" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test1","id":"0","b":0,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test4","id":"11","b":3,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test4","id":"13","b":3,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test1","id":"14","b":0,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test4","id":"17","b":3,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test4","id":"3","b":3,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test1","id":"4","b":0,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test4","id":"7","b":3,"createdAt":"2017-11-29 14:35:13.0"}""" concat "]")
    firstChunk.cursor.table should be(0)
    firstChunk.cursor.row should be(8)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2.toJson).await(5).convertTo[ResultFormat]

    JsArray(secondChunk.out.jsonElements).toString should be(
      "[" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model0","a":"test1","id":"8","b":0,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model1","a":"test2","id":"1","b":1,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model1","a":"test2","id":"15","b":1,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model1","a":"test2","id":"5","b":1,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model1","a":"test2","id":"9","b":1,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model2","a":"test3","id":"10","b":2,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model2","a":"test3","id":"12","b":2,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model2","a":"test3","id":"16","b":2,"createdAt":"2017-11-29 14:35:13.0"}""" concat "]")

    secondChunk.cursor.table should be(2)
    secondChunk.cursor.row should be(3)

    val request3   = request.copy(cursor = secondChunk.cursor)
    val thirdChunk = exporter.executeExport(dataResolver, request3.toJson).await(5).convertTo[ResultFormat]

    JsArray(thirdChunk.out.jsonElements).toString should be(
      "[" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model2","a":"test3","id":"2","b":2,"createdAt":"2017-11-29 14:35:13.0"},""" concat
        """{"updatedAt":"2017-12-05 12:34:23.0","_typeName":"Model2","a":"test3","id":"6","b":2,"createdAt":"2017-11-29 14:35:13.0"}""" concat "]")

    thirdChunk.cursor.table should be(-1)
    thirdChunk.cursor.row should be(-1)
  }

  "Exporting relationData" should "work (filesizelimit set to 1000)" in {
    val nodes =
      """{ "valueType": "nodes", "values": [
                  |{"_typeName": "Model0", "id": "0", "a": "test", "b":  0, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model1", "id": "1", "a": "test", "b":  1},
                  |{"_typeName": "Model2", "id": "2", "a": "test", "b":  2, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model0", "id": "3", "a": "test3", "b":  3, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model0", "id": "4", "a": "test4", "b":  4, "createdAt": "2017-11-29 14:35:13"}
                  |]}""".stripMargin.parseJson

    val relations =
      """{ "valueType": "relations", "values": [
        |[{"_typeName":"Model0","id":"0","fieldName":"relation0top"},{"_typeName":"Model0","id":"0","fieldName":"relation0bottom"}],
        |[{"_typeName":"Model0","id":"3","fieldName":"relation0top"},{"_typeName":"Model0","id":"3","fieldName":"relation0bottom"}],
        |[{"_typeName":"Model0","id":"4","fieldName":"relation0top"},{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"}],
        |[{"_typeName":"Model0","id":"3","fieldName":"relation0top"},{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"}],
        |[{"_typeName":"Model0","id":"0","fieldName":"relation0top"},{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"}],
        |[{"_typeName":"Model0","id":"0","fieldName":"relation0top"},{"_typeName":"Model0","id":"3","fieldName":"relation0bottom"}],
        |[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"0","fieldName":"model1"}],
        |[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"3","fieldName":"model1"}],
        |[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"4","fieldName":"model1"}],
        |[{"_typeName":"Model2","id":"2","fieldName":"model1"},{"_typeName":"Model1","id":"1","fieldName":"model2"}]
        |]}
        |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(relations).await(5)

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("relations", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      """[""" concat
        """[{"_typeName":"Model0","id":"0","fieldName":"relation0bottom"},{"_typeName":"Model0","id":"0","fieldName":"relation0top"}],""" concat
        """[{"_typeName":"Model0","id":"3","fieldName":"relation0bottom"},{"_typeName":"Model0","id":"3","fieldName":"relation0top"}],""" concat
        """[{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"},{"_typeName":"Model0","id":"4","fieldName":"relation0top"}],""" concat
        """[{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"},{"_typeName":"Model0","id":"3","fieldName":"relation0top"}],""" concat
        """[{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"},{"_typeName":"Model0","id":"0","fieldName":"relation0top"}],""" concat
        """[{"_typeName":"Model0","id":"3","fieldName":"relation0bottom"},{"_typeName":"Model0","id":"0","fieldName":"relation0top"}],""" concat
        """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"0","fieldName":"model1"}],""" concat
        """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"3","fieldName":"model1"}]""" concat "]")
    firstChunk.cursor.table should be(1)
    firstChunk.cursor.row should be(2)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2.toJson).await(5).convertTo[ResultFormat]
    JsArray(secondChunk.out.jsonElements).toString should be(
      """[""" concat
        """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"4","fieldName":"model1"}],""" concat
        """[{"_typeName":"Model1","id":"1","fieldName":"model2"},{"_typeName":"Model2","id":"2","fieldName":"model1"}]""" concat "]")

    secondChunk.cursor.table should be(-1)
    secondChunk.cursor.row should be(-1)
  }

  "Exporting ListValues" should "work" in {

    val nodes =
      """{"valueType": "nodes", "values": [
                  |{"_typeName": "Model0", "id": "0", "a": "test", "b":  0, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model1", "id": "1", "a": "test", "b":  1},
                  |{"_typeName": "Model2", "id": "2", "a": "test", "b":  2, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model0", "id": "3", "a": "test", "b":  3},
                  |{"_typeName": "Model1", "id": "4", "a": "test", "b":  3}
                  |]}""".stripMargin.parseJson

    val lists =
      """{"valueType": "lists", "values": [
        |{"_typeName": "Model1", "id": "1", "listField": [0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99]},
        |{"_typeName": "Model1", "id": "1", "listField": [100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199]},
        |{"_typeName": "Model1", "id": "1", "listField": [200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299]}
        |]}
        |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(lists).await(5)

    val cursor     = Cursor(0, 0, 0, 0)
    val request    = ExportRequest("lists", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request.toJson).await(5).convertTo[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      """[{"_typeName":"Model1","id":"1","listField":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99]},{"_typeName":"Model1","id":"1","listField":[100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199]},{"_typeName":"Model1","id":"1","listField":[200,201,202,203,204,205,206,207,208,209]},{"_typeName":"Model1","id":"1","listField":[210,211,212,213,214,215,216,217,218,219]},{"_typeName":"Model1","id":"1","listField":[220]}]""")
    firstChunk.cursor.table should be(0)
    firstChunk.cursor.row should be(0)
    firstChunk.cursor.field should be(0)
    firstChunk.cursor.array should be(221)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2.toJson).await(5).convertTo[ResultFormat]
    JsArray(secondChunk.out.jsonElements).toString should be(
      """[{"_typeName":"Model1","id":"1","listField":[221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299]}]""")

    secondChunk.cursor.table should be(-1)
    secondChunk.cursor.row should be(-1)
    secondChunk.cursor.field should be(-1)
    secondChunk.cursor.array should be(-1)
  }
}
