package com.prisma.api.import_export

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.DataResolver
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

class RelationImportErrorHandlingSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {

  lazy val project = SchemaDsl.fromStringV11() {
    """
      |type Model0 {
      |  id: ID! @id
      |  a: String
      |  model0self: Model0 @relation(link: TABLE)
      |}
      |
      |type Model1 {
      |  id: ID! @id
      |  a: String
      |  model0: Model0 @relation(link: TABLE)
      |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  lazy val importer                   = new BulkImport(project)
  lazy val exporter                   = new BulkExport(project)
  lazy val dataResolver: DataResolver = this.dataResolver(project)

  // todo postgres can't do partial success of batches
  "Importing relations between non-existing models" should "return a proper error" ignore {

    val nodes = """{"valueType": "nodes", "values": [
                    |{"_typeName": "Model0", "id": "0", "a": "test"},
                    |{"_typeName": "Model1", "id": "1", "a": "test"}
                    |]}""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
          |[{"_typeName": "Model0", "id": "0", "fieldName": null},{"_typeName": "Model1", "id": "7", "fieldName": "model0"}],
          |[{"_typeName": "Model0", "id": "0", "fieldName": null},{"_typeName": "Model1", "id": "1", "fieldName": "model0"}],
          |[{"_typeName": "Model0", "id": "7", "fieldName": null},{"_typeName": "Model0", "id": "1", "fieldName": "model0self"}]
          |]}
          |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    val res = importer.executeImport(relations).await(5)

    println(res)

    res.toString should include("""Failure inserting into relationtable _Relation0to0 with ids 1 and 7.""")
    res.toString should include("""Failure inserting into relationtable _Relation0to1 with ids 7 and 0.""")

    val res0 = server.query("query{model0s{id, a}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"0","a":"test"}]}}""")

    val res1 = server.query("query{model1s{id, a}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"1","a":"test"}]}}""")

    server.query("query{model0s{id, model0self{id}}}", project).toString should be("""{"data":{"model0s":[{"id":"0","model0self":null}]}}""")
    server.query("query{model1s{id, model0{id}}}", project).toString should be("""{"data":{"model1s":[{"id":"1","model0":{"id":"0"}}]}}""")
  }
}
