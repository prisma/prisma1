package com.prisma.api.import_export

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.ImportExportCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.utils.await.AwaitUtils
import org.scalatest.{FlatSpec, Matchers}

class BulkImportSpec extends FlatSpec with Matchers with ApiSpecBase with AwaitUtils {
  override def runOnlyForCapabilities = Set(ImportExportCapability)

  lazy val project: Project = SchemaDsl.fromStringv11() { """
                                                      |type Model0 {
                                                      |   id: ID! @id
                                                      |   a: String
                                                      |   b: Int
                                                      |   createdAt: DateTime! @createdAt
                                                      |   relation0top: Model0 @relation(name:"Relation0", link: TABLE)
                                                      |   relation0bottom: Model0 @relation(name:"Relation0")
                                                      |   model1: Model1 @relation(name:"Relation1", link: TABLE)
                                                      |}
                                                      |
                                                      |type Model1 {
                                                      |   id: ID! @id
                                                      |   a: String
                                                      |   b: Int
                                                      |   createdAt: DateTime! @createdAt
                                                      |   listField: [Int!]! @scalarList(strategy: RELATION)
                                                      |   model0: Model0 @relation(name:"Relation1")
                                                      |   model2: Model2 @relation(name:"Relation2", link: TABLE)
                                                      |}
                                                      |
                                                      |type Model2 {
                                                      |   id: ID! @id
                                                      |   a: String
                                                      |   b: Int
                                                      |   createdAt: DateTime! @createdAt
                                                      |   name: String
                                                      |   model1: Model1 @relation(name:"Relation2")
                                                      |}
                                                      |""".stripMargin }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  val importer = new BulkImport(project)

  "Combining the data from the three files" should "work" in {

    val nodes = """{"valueType": "nodes", "values": [
                    |{"_typeName": "Model0", "id": "0", "a": "test", "b":  0, "createdAt": "2017-11-29 14:35:13", "updatedAt": "2017-12-29 14:35:13"},
                    |{"_typeName": "Model1", "id": "1", "a": "test", "b":  1},
                    |{"_typeName": "Model2", "id": "2", "a": "test", "b":  2, "createdAt": "2017-11-29 14:35:13"},
                    |{"_typeName": "Model0", "id": "3", "a": "test", "b":  3}
                    |]}""".stripMargin.parseJson

    val relations =
      """{"valueType":"relations", "values": [
          |[{"_typeName": "Model0", "id": "0", "fieldName": "relation0top"},{"_typeName": "Model0", "id": "0", "fieldName": "relation0bottom"}],
          |[{"_typeName": "Model1", "id": "1", "fieldName": "model0"},{"_typeName": "Model0", "id": "0", "fieldName": "model1"}],
          |[{"_typeName": "Model2", "id": "2", "fieldName": "model1"},{"_typeName": "Model1", "id": "1", "fieldName": "model2"}],
          |[{"_typeName": "Model0", "id": "3", "fieldName": "relation0top"},{"_typeName": "Model0", "id": "3", "fieldName": "relation0bottom"}]
          |]}
          |""".stripMargin.parseJson

    val lists = """{ "valueType": "lists", "values": [
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]},
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]},
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]}
                    |]}
                    |""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)
    importer.executeImport(relations).await(5)
    importer.executeImport(lists).await(5)

    val res0 = server.query("query{model0s{id, a, b}}", project).toString
    res0 should be("""{"data":{"model0s":[{"id":"0","a":"test","b":0},{"id":"3","a":"test","b":3}]}}""")

    val res1 = server.query("query{model1s{id, a, b, listField}}", project).toString
    res1 should be("""{"data":{"model1s":[{"id":"1","a":"test","b":1,"listField":[2,3,4,5,2,3,4,5,2,3,4,5]}]}}""")

    val res2 = server.query("query{model2s{id, a, b, name}}", project).toString
    res2 should be("""{"data":{"model2s":[{"id":"2","a":"test","b":2,"name":null}]}}""")

    val rel0 = server.query("query{model0s{id, model1{id}, relation0top{id}, relation0bottom{id}}}", project).toString
    rel0 should be(
      """{"data":{"model0s":[{"id":"0","model1":{"id":"1"},"relation0top":{"id":"0"},"relation0bottom":{"id":"0"}},{"id":"3","model1":null,"relation0top":{"id":"3"},"relation0bottom":{"id":"3"}}]}}""")

    val rel1 = server.query("query{model1s{id, model0{id}, model2{id}}}", project).toString
    rel1 should be("""{"data":{"model1s":[{"id":"1","model0":{"id":"0"},"model2":{"id":"2"}}]}}""")

    val rel2 = server.query("query{model2s{id, model1{id}}}", project).toString
    rel2 should be("""{"data":{"model2s":[{"id":"2","model1":{"id":"1"}}]}}""")
  }

  "Inserting a single node with a field with a String value" should "work" in {
    val nodes = """{ "valueType": "nodes", "values": [{"_typeName": "Model0", "id": "just-some-id", "a": "test"}]}""".parseJson
    importer.executeImport(nodes).await(5)

    val res = server.query("query{model0s{id, a}}", project)
    res.toString should be("""{"data":{"model0s":[{"id":"just-some-id","a":"test"}]}}""")
  }

  "Inserting several nodes with a field with a Int value" should "work" in {

    val nodes = """{"valueType":"nodes","values":[
                  |{"_typeName": "Model0", "id": "just-some-id", "b": 12},
                  |{"_typeName": "Model0", "id": "just-some-id2", "b": 13}
                  ]}""".stripMargin.parseJson

    importer.executeImport(nodes).await(5)

    val res = server.query("query{model0s{id, b}}", project)
    res.toString should be("""{"data":{"model0s":[{"id":"just-some-id","b":12},{"id":"just-some-id2","b":13}]}}""")
  }

  "Inserting a node with values for fields that do not exist" should "return the invalid index but keep on creating" in {

    val nodes = """{"valueType":"nodes","values":[
                  |{"_typeName": "Model0", "id": "just-some-id0", "b": 12},
                  |{"_typeName": "Model0", "id": "just-some-id3", "c": 12},
                  |{"_typeName": "Model0", "id": "just-some-id2", "b": 13}
                  ]}""".stripMargin.parseJson

    val res2 = importer.executeImport(nodes).await(5)

    res2.toString should be("""["The model Model0 with id just-some-id3 has an unknown field 'c' in field list."]""")

    val res = server.query("query{model0s{id, b}}", project)

    res.toString should be("""{"data":{"model0s":[{"id":"just-some-id0","b":12},{"id":"just-some-id2","b":13}]}}""")
  }

  //todo postgres stops batch actions once an error occurs and rolls back
  // the order in which the items are created is not deterministic. therefore the error message can vary depending on which item is created last
  "Inserting a node with a duplicate id" should "return the invalid index but keep on creating" ignore {
    val nodes = """{"valueType":"nodes","values":[
                  |{"_typeName": "Model0", "id": "just-some-id4", "b": 12},
                  |{"_typeName": "Model0", "id": "just-some-id5", "b": 13},
                  |{"_typeName": "Model0", "id": "just-some-id5", "b": 15}
                  ]}""".stripMargin.parseJson

    val res2 = importer.executeImport(nodes).await(5)

    res2.toString should be(
      """["Failure inserting Model0 with Id: just-some-id5. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'just-some-id5' for key 'PRIMARY'","Failure inserting RelayRow with Id: just-some-id5. Cause: java.sql.SQLIntegrityConstraintViolationException: Duplicate entry 'just-some-id5' for key 'PRIMARY'"]""")
    val res = server.query("query{model0s{id, b}}", project)
    res.toString should (be("""{"data":{"model0s":[{"id":"just-some-id4","b":12},{"id":"just-some-id5","b":13}]}}""") or
      be("""{"data":{"model0s":[{"id":"just-some-id4","b":12},{"id":"just-some-id5","b":15}]}}"""))
  }
}
