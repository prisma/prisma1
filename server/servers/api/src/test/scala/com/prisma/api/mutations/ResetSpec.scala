package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.api.import_export.BulkImport
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

class ResetSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  val project: Project = SchemaDsl.fromBuilder { schema =>
    val model1: SchemaDsl.ModelBuilder = schema
      .model("Model1")
      .field("a", _.String)
      .field("b", _.Int)
      .field("listField", _.Int, isList = true)
      .field("createdAt", _.DateTime)
      .field("updatedAt", _.DateTime)

    val model0: SchemaDsl.ModelBuilder = schema
      .model("Model0")
      .field("a", _.String)
      .field("b", _.Int)
      .field("createdAt", _.DateTime)
      .field("updatedAt", _.DateTime)
      .oneToOneRelation("model1", "model0", model1, Some("Relation1"))

    model0.oneToOneRelation("relation0top", "relation0bottom", model0, Some("Relation0"))

    val model2: SchemaDsl.ModelBuilder = schema
      .model("Model2")
      .field("a", _.String)
      .field("b", _.Int)
      .field("name", _.String)
      .field("createdAt", _.DateTime)
      .field("updatedAt", _.DateTime)
      .oneToOneRelation("model1", "model2", model1, Some("Relation2"))
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  val importer = new BulkImport(project)

  "The ResetDataMutation" should "wipe all data" in {

    val nodes = """{"valueType": "nodes", "values": [
                    |{"_typeName": "Model0", "id": "0", "a": "test", "b":  0, "createdAt": "2017-11-29 14:35:13"},
                    |{"_typeName": "Model1", "id": "1", "a": "test", "b":  1},
                    |{"_typeName": "Model2", "id": "2", "a": "test", "b":  2, "createdAt": "2017-11-29 14:35:13"},
                    |{"_typeName": "Model0", "id": "3", "a": "test", "b":  3}
                    |]}""".stripMargin.parseJson

    val lists = """{ "valueType": "lists", "values": [
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]}
                    |]}
                    |""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
          |[{"_typeName": "Model0", "id": "0", "fieldName": "relation0top"},{"_typeName": "Model0", "id": "0", "fieldName": "relation0bottom"}],
          |[{"_typeName": "Model1", "id": "1", "fieldName": "model0"},{"_typeName": "Model0", "id": "0", "fieldName": "model1"}],
          |[{"_typeName": "Model2", "id": "2", "fieldName": "model1"},{"_typeName": "Model1", "id": "1", "fieldName": "model2"}],
          |[{"_typeName": "Model0", "id": "3", "fieldName": "relation0top"},{"_typeName": "Model0", "id": "3", "fieldName": "relation0bottom"}]
          |]}
          |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(lists).await(5)
    importer.executeImport(relations).await(5)

    val res0 = server.query("query{model0s{id, a, b}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"0","a":"test","b":0},{"id":"3","a":"test","b":3}]}}""")

    val res1 = server.query("query{model1s{id, a, b, listField}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"1","a":"test","b":1,"listField":[2,3,4,5]}]}}""")

    val res2 = server.query("query{model2s{id, a, b, name}}", project).toString
    res2 should be("""{"data":{"model2s":[{"id":"2","a":"test","b":2,"name":null}]}}""")

    val rel0 = server.query("query{model0s{id, model1{id}, relation0top{id}, relation0bottom{id}}}", project).toString
    rel0 should be(
      """{"data":{"model0s":[{"id":"0","model1":{"id":"1"},"relation0top":{"id":"0"},"relation0bottom":{"id":"0"}},{"id":"3","model1":null,"relation0top":{"id":"3"},"relation0bottom":{"id":"3"}}]}}""")

    val rel1 = server.query("query{model1s{id, model0{id}, model2{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"1","model0":{"id":"0"},"model2":{"id":"2"}}]}}""")

    val rel2 = server.query("query{model2s{id, model1{id}}}", project).toString
    rel2 should be("""{"data":{"model2s":[{"id":"2","model1":{"id":"1"}}]}}""")

    val result = server.queryPrivateSchema("mutation{resetData}", project)
    result.pathAsBool("data.resetData") should equal(true)

    server.query("query{model0s{id}}", project, dataContains = """{"model0s":[]}""")
    server.query("query{model1s{id}}", project, dataContains = """{"model1s":[]}""")
    server.query("query{model2s{id}}", project, dataContains = """{"model2s":[]}""")

    ifConnectorIsActive {
      ifConnectorIsActiveAndNotSqliteNative {
        dataResolver(project).countByTable("_RelayId").await should be(0)
      }
      dataResolver(project).countByTable("_Relation0").await should be(0)
      dataResolver(project).countByTable("_Relation1").await should be(0)
      dataResolver(project).countByTable("_Relation2").await should be(0)
    }
  }

  "The ResetDataMutation" should "reinstate foreign key constraints again after wiping the data" ignore {

    val nodes = """{"valueType": "nodes", "values": [
                  |{"_typeName": "Model0", "id": "0", "a": "test", "b":  0, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model1", "id": "1", "a": "test", "b":  1},
                  |{"_typeName": "Model2", "id": "2", "a": "test", "b":  2, "createdAt": "2017-11-29 14:35:13"},
                  |{"_typeName": "Model0", "id": "3", "a": "test", "b":  3}
                  |]}""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)

    val result = server.queryPrivateSchema("mutation{resetData}", project)
    result.pathAsBool("data.resetData") should equal(true)

    server.query("query{model0s{id}}", project, dataContains = """{"model0s":[]}""")
    server.query("query{model1s{id}}", project, dataContains = """{"model1s":[]}""")
    server.query("query{model2s{id}}", project, dataContains = """{"model2s":[]}""")

    ifConnectorIsActiveAndNotSqliteNative { dataResolver(project).countByTable("_RelayId").await should be(0) }

    import slick.jdbc.PostgresProfile.api._
    val insert = sql"INSERT INTO `#${project.id}`.`_Relation1` VALUES ('someID', 'a', 'b')"

//    intercept[PSQLException] { database.runDbActionOnClientDb(insert.asUpdate) }
  }
}
