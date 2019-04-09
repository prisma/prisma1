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

class BulkExportSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  val project = SchemaDsl.fromStringV11() {
    s"""
      |type Model0 {
      |  id: ID! @id
      |  createdAt: DateTime! @createdAt
      |  updatedAt: DateTime! @updatedAt
      |  a: String
      |  b: Int
      |  relation0top: [Model0] @relation(name: "MyRelation")
      |  relation0bottom: [Model0] @relation(name: "MyRelation")
      |  model1: [Model1]
      |}
      |
      |type Model1 {
      |  id: ID! @id
      |  createdAt: DateTime! @createdAt
      |  updatedAt: DateTime! @updatedAt
      |  a: String
      |  b: Int
      |  listField: [Int] $scalarListDirective
      |  model0: [Model0]
      |  model2: [Model2]
      |}
      |
      |type Model2 {
      |  id: ID! @id
      |  createdAt: DateTime! @createdAt
      |  updatedAt: DateTime! @updatedAt
      |  a: String
      |  b: Int
      |  name: String
      |  model1: [Model1]
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

  "Exporting nodes" should "work (with filesize limit set to 1000 for test)" in {

    val nodes =
      """{ "valueType": "nodes", "values": [
        |{"_typeName": "Model0", "id": "0","a": "test1", "b": 0, "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model1", "id": "1","a": "test2", "b": 1, "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model2", "id": "2", "a": "test3", "b": 2, "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "3", "a": "test4", "b": 3,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "4", "a": "test1", "b": 0,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model1", "id": "5", "a": "test2", "b": 1,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model2", "id": "6", "a": "test3", "b": 2,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "7", "a": "test4", "b": 3,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "8", "a": "test1", "b": 0,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model1", "id": "9", "a": "test2", "b": 1,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model2", "id": "10", "a": "test3", "b": 2,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "11", "a": "test4", "b": 3,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model2", "id": "12", "a": "test3", "b": 2,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "13", "a": "test4", "b": 3,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "14", "a": "test1", "b": 0,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model1", "id": "15", "a": "test2", "b": 1,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model2", "id": "16", "a": "test3", "b": 2,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"},
        |{"_typeName": "Model0", "id": "17", "a": "test4", "b": 3,  "createdAt": "2017-11-29T14:35:13.000Z", "updatedAt":"2017-12-05T12:34:23.000Z"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await(5).toString should be("[]")

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("nodes", cursor)
    val temp0      = exporter.executeExport(dataResolver, request).await(5)
    val firstChunk = temp0.as[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      "[" ++
        """{"_typeName":"Model0","id":"0","updatedAt":"2017-12-05T12:34:23.000Z","a":"test1","b":0,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"11","updatedAt":"2017-12-05T12:34:23.000Z","a":"test4","b":3,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"13","updatedAt":"2017-12-05T12:34:23.000Z","a":"test4","b":3,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"14","updatedAt":"2017-12-05T12:34:23.000Z","a":"test1","b":0,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"17","updatedAt":"2017-12-05T12:34:23.000Z","a":"test4","b":3,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"3","updatedAt":"2017-12-05T12:34:23.000Z","a":"test4","b":3,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"4","updatedAt":"2017-12-05T12:34:23.000Z","a":"test1","b":0,"createdAt":"2017-11-29T14:35:13.000Z"}""" ++ "]")
    firstChunk.cursor.table should be(0)
    firstChunk.cursor.row should be(7)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val temp        = exporter.executeExport(dataResolver, request2).await(5)
    val secondChunk = temp.as[ResultFormat]

    JsArray(secondChunk.out.jsonElements).toString should be(
      "[" ++
        """{"_typeName":"Model0","id":"7","updatedAt":"2017-12-05T12:34:23.000Z","a":"test4","b":3,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model0","id":"8","updatedAt":"2017-12-05T12:34:23.000Z","a":"test1","b":0,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model1","id":"1","updatedAt":"2017-12-05T12:34:23.000Z","a":"test2","b":1,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model1","id":"15","updatedAt":"2017-12-05T12:34:23.000Z","a":"test2","b":1,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model1","id":"5","updatedAt":"2017-12-05T12:34:23.000Z","a":"test2","b":1,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model1","id":"9","updatedAt":"2017-12-05T12:34:23.000Z","a":"test2","b":1,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model2","id":"10","updatedAt":"2017-12-05T12:34:23.000Z","a":"test3","b":2,"createdAt":"2017-11-29T14:35:13.000Z"}""" ++ "]")

    secondChunk.cursor.table should be(2)
    secondChunk.cursor.row should be(1)

    val request3   = request.copy(cursor = secondChunk.cursor)
    val thirdChunk = exporter.executeExport(dataResolver, request3).await(5).as[ResultFormat]

    JsArray(thirdChunk.out.jsonElements).toString should be(
      "[" ++
        """{"_typeName":"Model2","id":"12","updatedAt":"2017-12-05T12:34:23.000Z","a":"test3","b":2,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model2","id":"16","updatedAt":"2017-12-05T12:34:23.000Z","a":"test3","b":2,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model2","id":"2","updatedAt":"2017-12-05T12:34:23.000Z","a":"test3","b":2,"createdAt":"2017-11-29T14:35:13.000Z"},""" ++
        """{"_typeName":"Model2","id":"6","updatedAt":"2017-12-05T12:34:23.000Z","a":"test3","b":2,"createdAt":"2017-11-29T14:35:13.000Z"}""" ++ "]")

    thirdChunk.cursor.table should be(-1)
    thirdChunk.cursor.row should be(-1)
  }

  // TODO: this must be validated by do4gr
  "Exporting relationData with Postgres" should "work (filesizelimit set to 1000)" in {
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

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("relations", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request).await(5).as[ResultFormat]

    firstChunk.out.jsonElements should have(size(8))
    firstChunk.out.jsonElements should contain theSameElementsAs Vector(
      """[{"_typeName":"Model2","id":"2","fieldName":"model1"},{"_typeName":"Model1","id":"1","fieldName":"model2"}]""".parseJson,
      """[{"_typeName":"Model0","id":"0","fieldName":"relation0top"},{"_typeName":"Model0","id":"0","fieldName":"relation0bottom"}]""".parseJson,
      """[{"_typeName":"Model0","id":"3","fieldName":"relation0top"},{"_typeName":"Model0","id":"3","fieldName":"relation0bottom"}]""".parseJson,
      """[{"_typeName":"Model0","id":"0","fieldName":"relation0top"},{"_typeName":"Model0","id":"3","fieldName":"relation0bottom"}]""".parseJson,
      """[{"_typeName":"Model0","id":"4","fieldName":"relation0top"},{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"}]""".parseJson,
      """[{"_typeName":"Model0","id":"3","fieldName":"relation0top"},{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"}]""".parseJson,
      """[{"_typeName":"Model0","id":"0","fieldName":"relation0top"},{"_typeName":"Model0","id":"4","fieldName":"relation0bottom"}]""".parseJson,
      """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"0","fieldName":"model1"}]""".parseJson
    )
    firstChunk.cursor.table should be(2)
    firstChunk.cursor.row should be(1)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2).await(5).as[ResultFormat]
    secondChunk.out.jsonElements should have(size(2))
    secondChunk.out.jsonElements should contain theSameElementsAs Vector(
      """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"3","fieldName":"model1"}]""".parseJson,
      """[{"_typeName":"Model1","id":"1","fieldName":"model0"},{"_typeName":"Model0","id":"4","fieldName":"model1"}]""".parseJson
    )

    secondChunk.cursor.table should be(-1)
    secondChunk.cursor.row should be(-1)
  }

  //List Value chunking is simplified for now. We will have up to 1000 list values for one id as the smallest inseparable chunk. if that is over 1mb we won't split it
  "Exporting ListValues" should "work" ignore {

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

    val cursor  = Cursor(0, 0)
    val request = ExportRequest("lists", cursor)

    val result = exporter.executeExport(dataResolver, request).await(5)
    println(result)
    val firstChunk = result.as[ResultFormat]

    println(firstChunk.cursor)
    JsArray(firstChunk.out.jsonElements).toString should be(
      """[{"_typeName":"Model1","id":"1","listField":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99]},{"_typeName":"Model1","id":"1","listField":[100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199]},{"_typeName":"Model1","id":"1","listField":[200,201,202,203,204,205,206,207,208,209]},{"_typeName":"Model1","id":"1","listField":[210,211,212,213,214,215,216,217,218,219]},{"_typeName":"Model1","id":"1","listField":[220]}]""")
    firstChunk.cursor.table should be(0)
    firstChunk.cursor.row should be(221)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2).await(5).as[ResultFormat]
    JsArray(secondChunk.out.jsonElements).toString should be(
      """[{"_typeName":"Model1","id":"1","listField":[221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299]}]""")

    secondChunk.cursor.table should be(-1)
    secondChunk.cursor.row should be(-1)
  }

  "Exporting ListValues" should "work without chunking" in {

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

    val cursor  = Cursor(0, 0)
    val request = ExportRequest("lists", cursor)

    val result = exporter.executeExport(dataResolver, request).await(5)
    println(result)
    val firstChunk = result.as[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      """[{"_typeName":"Model1","id":"1","listField":[0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32,33,34,35,36,37,38,39,40,41,42,43,44,45,46,47,48,49,50,51,52,53,54,55,56,57,58,59,60,61,62,63,64,65,66,67,68,69,70,71,72,73,74,75,76,77,78,79,80,81,82,83,84,85,86,87,88,89,90,91,92,93,94,95,96,97,98,99,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,200,201,202,203,204,205,206,207,208,209,210,211,212,213,214,215,216,217,218,219,220,221,222,223,224,225,226,227,228,229,230,231,232,233,234,235,236,237,238,239,240,241,242,243,244,245,246,247,248,249,250,251,252,253,254,255,256,257,258,259,260,261,262,263,264,265,266,267,268,269,270,271,272,273,274,275,276,277,278,279,280,281,282,283,284,285,286,287,288,289,290,291,292,293,294,295,296,297,298,299]}]""")
    firstChunk.cursor.table should be(0)
    firstChunk.cursor.row should be(1)

  }
}
