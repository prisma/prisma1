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
import play.api.libs.json._

class ListValueImportExportSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  val project2: Project = SchemaDsl.fromBuilder { schema =>
    val enum = schema.enum("Enum", Vector("AB", "CD", "\uD83D\uDE0B", "\uD83D\uDCA9"))

    schema
      .model("Model0")
      .field("a", _.String)
      .field("stringList", _.String, isList = true)
      .field("intList", _.Int, isList = true)
      .field("floatList", _.Float, isList = true)
      .field("booleanList", _.Boolean, isList = true)

    schema
      .model("Model1")
      .field("a", _.String)
      .field("enumList", _.Enum, isList = true, enum = Some(enum))
      .field("datetimeList", _.DateTime, isList = true)
      .field("jsonList", _.Json, isList = true)
  }

  val baseProject = SchemaDsl.fromStringV11() {
    s"""
      |type Model0 {
      |  id: ID! @id
      |  a: String
      |  stringList: [String] $scalarListDirective 
      |  intList: [Int] $scalarListDirective
      |  floatList: [Float] $scalarListDirective
      |  booleanList: [Boolean] $scalarListDirective
      |}
      |
      |type Model1 {
      |  id: ID! @id
      |  a: String
      |  enumList: [Enum] $scalarListDirective
      |  datetimeList: [DateTime] $scalarListDirective
      |  jsonList: [Json] $scalarListDirective
      |}
      |
      |enum Enum {
      |  AB
      |  CD
      |}
    """.stripMargin
  }

  // work around to allow emojis in enum values
  val enum        = baseProject.schema.enums.head
  val emojiedEnum = enum.copy(values = enum.values ++ Vector("\uD83D\uDE0B", "\uD83D\uDCA9"))
  val project = baseProject.copy(
    schema = baseProject.schema.copy(
      enums = List(emojiedEnum),
      modelTemplates = baseProject.schema.modelTemplates.map { model =>
        model.copy(
          fieldTemplates = model.fieldTemplates.map { field =>
            field.enum match {
              case Some(_) => field.copy(enum = Some(emojiedEnum))
              case None    => field
            }
          }
        )
      }
    )
  )

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)
  val importer                    = new BulkImport(project)
  val exporter                    = new BulkExport(project)
  val dataResolver: DataResolver  = this.dataResolver(project)

  "Importing ListValues for a wrong Id" should "fail" in {

    val nodes =
      """{ "valueType": "nodes", "values": [
        |{"_typeName": "Model0", "id": "0","a": "test1"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await().toString should be("[]")

    val lists =
      """{"valueType": "lists", "values": [
        |{"_typeName": "Model0", "id": "3", "stringList": ["Just", "a" , "bunch", "of" ,"strings"]}
        |]}
        |""".stripMargin.parseJson

    val model = project.schema.getModelByName_!("Model0")
    val field = model.getFieldByName_!("stringList")
    val res   = importer.executeImport(lists).await().toString

    ifConnectorIsNotSQLite(res should include(s"Failure inserting into listTable ${model.name}_${field.name} for the id 3 for value "))
    ifConnectorIsSQLite(res should include(
      s"Failure inserting into listTable ${model.name}_${field.name}: Cause:[SQLITE_CONSTRAINT_FOREIGNKEY]  A foreign key constraint failed (FOREIGN KEY constraint failed)"))

  }

  "Exporting nodes" should "work (with filesize limit set to 1000 for test) and preserve the order of items" in {

    val nodes =
      """{ "valueType": "nodes", "values": [
        |{"_typeName": "Model0", "id": "0", "a": "test1"},
        |{"_typeName": "Model0", "id": "1", "a": "test4"},
        |{"_typeName": "Model1", "id": "2", "a": "test2"},
        |{"_typeName": "Model1", "id": "3", "a": "test2"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await().toString should be("[]")

    val lists =
      """{"valueType": "lists", "values": [
        |{"_typeName": "Model0", "id": "0", "stringList": ["Just", "a" , "bunch", "of" ,"strings"]},
        |{"_typeName": "Model0", "id": "0", "intList": [100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199]},
        |{"_typeName": "Model0", "id": "1", "floatList": [1.423423, 3.1234324234, 4.23432424, 4.234234324234]},
        |{"_typeName": "Model0", "id": "1", "booleanList": [true, true, false, false, true, true]} 
        |]}""".stripMargin.parseJson

    val lists2 =
      """{"valueType": "lists", "values": [
        |{"_typeName": "Model0", "id": "1", "booleanList": [false, false, false, false, false, false]},
        |{"_typeName": "Model0", "id": "0", "intList": [100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199]},
        |{"_typeName": "Model0", "id": "0", "stringList": ["Just", "a" , "bunch", "of" ,"strings"]},
        |{"_typeName": "Model0", "id": "1", "floatList": [1.423423, 3.1234324234, 4.23432424, 4.234234324234]},
        |{"_typeName": "Model0", "id": "1", "booleanList": [true, true, false, false, true, true]}
        |]}""".stripMargin.parseJson

    importer.executeImport(lists).await().toString should be("[]")
    importer.executeImport(lists2).await().toString should be("[]")

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("lists", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request).await().as[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      "[" ++
        """{"_typeName":"Model0","id":"0","stringList":["Just","a","bunch","of","strings","Just","a","bunch","of","strings"]},""" ++
        """{"_typeName":"Model0","id":"0","intList":[100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199,100,101,102,103,104,105,106,107,108,109,110,111,112,113,114,115,116,117,118,119,120,121,122,123,124,125,126,127,128,129,130,131,132,133,134,135,136,137,138,139,140,141,142,143,144,145,146,147,148,149,150,151,152,153,154,155,156,157,158,159,160,161,162,163,164,165,166,167,168,169,170,171,172,173,174,175,176,177,178,179,180,181,182,183,184,185,186,187,188,189,190,191,192,193,194,195,196,197,198,199]}""" ++
        "]")
    firstChunk.cursor.table should be(2)
    firstChunk.cursor.row should be(0)

    val request2    = request.copy(cursor = firstChunk.cursor)
    val secondChunk = exporter.executeExport(dataResolver, request2).await().as[ResultFormat]

    JsArray(secondChunk.out.jsonElements).toString should be("[" ++
      """{"_typeName":"Model0","id":"1","floatList":[1.423423,3.1234324234,4.23432424,4.234234324234,1.423423,3.1234324234,4.23432424,4.234234324234]},""" ++
      """{"_typeName":"Model0","id":"1","booleanList":[true,true,false,false,true,true,false,false,false,false,false,false,true,true,false,false,true,true]}""" ++
      "]")

    secondChunk.cursor.table should be(-1)
    secondChunk.cursor.row should be(-1)
  }

  "Exporting nodes" should "work (with filesize limit set to 1000 for test) for datetime and enum too and preserve the order of items" in {

    val nodes =
      """{ "valueType": "nodes", "values": [
        |{"_typeName": "Model0", "id": "0", "a": "test1"},
        |{"_typeName": "Model0", "id": "1", "a": "test4"},
        |{"_typeName": "Model1", "id": "2", "a": "test2"},
        |{"_typeName": "Model1", "id": "3", "a": "test2"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await().toString should be("[]")

    val lists =
      """{"valueType": "lists", "values": [
        |{"_typeName": "Model1", "id": "2", "enumList": ["AB", "CD", "\uD83D\uDE0B", "\uD83D\uDE0B", "😋"]},
        |{"_typeName": "Model1", "id": "2", "datetimeList": ["2017-12-05T12:34:23.000Z", "2018-12-05T12:34:23.000Z", "2018-01-04T17:36:41Z"]}
        |]}
        |""".stripMargin.parseJson

    importer.executeImport(lists).await().toString should be("[]")

    val cursor     = Cursor(0, 0)
    val request    = ExportRequest("lists", cursor)
    val firstChunk = exporter.executeExport(dataResolver, request).await().as[ResultFormat]

    JsArray(firstChunk.out.jsonElements).toString should be(
      "[" ++
        """{"_typeName":"Model1","id":"2","enumList":["AB","CD","😋","😋","😋"]},""" ++
        """{"_typeName":"Model1","id":"2","datetimeList":["2017-12-05T12:34:23.000Z","2018-12-05T12:34:23.000Z","2018-01-04T17:36:41.000Z"]}""" ++
        "]")
    firstChunk.cursor.table should be(-1)
    firstChunk.cursor.row should be(-1)
  }

  "Exporting nodes" should "work (with filesize limit set to 1000 for test) for json too and preserve the order of items" in {

    val nodes =
      """{ "valueType": "nodes", "values": [
        |{"_typeName": "Model0", "id": "0", "a": "test1"},
        |{"_typeName": "Model0", "id": "1", "a": "test4"},
        |{"_typeName": "Model1", "id": "2", "a": "test2"},
        |{"_typeName": "Model1", "id": "3", "a": "test2"}
        |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await().toString should be("[]")

    val jsonString =
      """{"_typeName":"Model1","id":"2","jsonList":[[{"_typeName":"STRING","id":"STRING","fieldName":"STRING"},{"_typeName":"STRING","id":"STRING","fieldName":"STRING"}]]}"""

    val lists = s"""{"valueType": "lists", "values": [$jsonString]}""".stripMargin.parseJson

    importer.executeImport(lists).await().toString should be("[]")

    val cursor       = Cursor(0, 0)
    val request      = ExportRequest("lists", cursor)
    val exportResult = exporter.executeExport(dataResolver, request).await()
    val firstChunk   = exportResult.as[ResultFormat]

    println(firstChunk.out.jsonElements)

    JsArray(firstChunk.out.jsonElements).toString should be("[" ++ s"""$jsonString""" ++ "]")
    firstChunk.cursor.table should be(-1)
    firstChunk.cursor.row should be(-1)
  }

}
