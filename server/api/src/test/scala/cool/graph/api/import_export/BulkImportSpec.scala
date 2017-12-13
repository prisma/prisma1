package cool.graph.api.mutations

  import cool.graph.api.ApiBaseSpec
  import cool.graph.api.database.import_export.BulkImport
  import cool.graph.shared.project_dsl.SchemaDsl
  import cool.graph.utils.await.AwaitUtils
  import org.scalatest.{FlatSpec, Matchers}
  import spray.json._

class BulkImportSpec extends FlatSpec with Matchers with ApiBaseSpec with AwaitUtils{

  val project = SchemaDsl() { schema =>
    schema
      .model("Model0")
      .field("a", _.String)
      .field("b", _.Int)

    schema
      .model("Model1")
      .field("a", _.String)
      .field("b", _.Int)
      .field("listField", _.Int, isList = true)

    schema
      .model("Model2")
      .field("a", _.String)
      .field("b", _.Int)
      .field("name", _.String)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }

    "Combining the data from the three files" should "work" in {

      val nodes = """{"valueType": "nodes", "values": [
                    |{"_typeName": "Model0", "id": "0", "a": "test", "b":  0, "createdAt": "2017-11-29 14:35:13"},
                    |{"_typeName": "Model1", "id": "1", "a": "test", "b":  1},
                    |{"_typeName": "Model2", "id": "2", "a": "test", "b":  2, "createdAt": "2017-11-29 14:35:13"},
                    |{"_typeName": "Model0", "id": "3", "a": "test", "b":  3}
                    |]}""".stripMargin.parseJson


      val lists = """{ "valueType": "lists", "values": [
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]},
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]},
                    |{"_typeName": "Model1", "id": "1", "listField": [2,3,4,5]}
                    |]}
                    |""".stripMargin.parseJson

      val importer = new BulkImport(project)

      importer.executeImport(nodes).await(5)
      importer.executeImport(lists).await(5)

      val res0 = server.executeQuerySimple("query{model0s{id, a, b}}", project).toString
      res0 should be("""{"data":{"model0s":[{"id":"0","a":"test","b":0},{"id":"3","a":"test","b":3}]}}""")

      val res1 = server.executeQuerySimple("query{model1s{id, a, b, listField}}", project).toString
      res1 should be("""{"data":{"model1s":[{"id":"1","a":"test","b":1,"listField":[2,3,4,5,2,3,4,5,2,3,4,5]}]}}""")

      val res2 = server.executeQuerySimple("query{model2s{id, a, b, name}}", project).toString
      res2 should be("""{"data":{"model2s":[{"id":"2","a":"test","b":2,"name":null}]}}""")
    }

    "Inserting a single node with a field with a String value" should "work" in {

      val types =
        s"""type Model0 @model {
           |  id: ID! @isUnique
           |  a: String
           |}""".stripMargin


      val nodes    = """{ "valueType": "nodes", "values": [
                  |{"_typeName": "Model0", "id": "just-some-id", "a": "test"}
                  ]}""".stripMargin.parseJson

      val importer = new BulkImport(project)
      importer.executeImport(nodes).await(5)

      val res = server.executeQuerySimple("query{allModel0s{id, a}}", project)
      res.toString should be("""{"data":{"allModel0s":[{"id":"just-some-id","a":"test"}]}}""")
    }
//
//    "Inserting a several nodes with a field with a Int value" should "work" in {
//      val (client, project1) = SchemaDsl.schema().buildEmptyClientAndProject(isEjected = true)
//      setupProject(client, project1)
//
//      val types =
//        s"""type Model0 @model {
//           |  id: ID! @isUnique
//           |  a: Int!
//           |}""".stripMargin
//
//      val refreshedProject = setupProjectForTest(types, client, project1)
//
//      val nodes = """{"valueType":"nodes","values":[
//                  |{"_typeName": "Model0", "id": "just-some-id", "a": 12},
//                  |{"_typeName": "Model0", "id": "just-some-id2", "a": 13}
//                  ]}""".stripMargin.parseJson
//
//      val importer = new BulkImport()
//      importer.executeImport(refreshedProject, nodes).await(5)
//
//      val res = executeQuerySimple("query{allModel0s{id, a}}", refreshedProject)
//      res.toString should be("""{"data":{"allModel0s":[{"id":"just-some-id","a":12},{"id":"just-some-id2","a":13}]}}""")
//    }
//
//    "Inserting a node with values for fields that do not exist" should "return the invalid index but keep on creating" in {
//      val (client, project1) = SchemaDsl.schema().buildEmptyClientAndProject(isEjected = true)
//      setupProject(client, project1)
//
//      val types =
//        s"""type Model0 @model {
//           |  id: ID! @isUnique
//           |  a: Int!
//           |}""".stripMargin
//
//      val refreshedProject = setupProjectForTest(types, client, project1)
//
//      val nodes = """{"valueType":"nodes","values":[
//                  |{"_typeName": "Model0", "id": "just-some-id0", "a": 12},
//                  |{"_typeName": "Model0", "id": "just-some-id3", "c": 12},
//                  |{"_typeName": "Model0", "id": "just-some-id2", "a": 13}
//                  ]}""".stripMargin.parseJson
//
//      val importer = new BulkImport()
//      val res2     = importer.executeImport(refreshedProject, nodes).await(5)
//
//      println(res2)
//
//      res2.toString should be("""[{"index":1,"message":" Unknown column 'c' in 'field list'"}]""")
//
//      val res = executeQuerySimple("query{allModel0s{id, a}}", refreshedProject)
//
//      res.toString should be("""{"data":{"allModel0s":[{"id":"just-some-id0","a":12},{"id":"just-some-id2","a":13}]}}""")
//    }
//
//    // the order in which the items are created is not deterministic. therefore the error message can vary depending on which item is created last
//    "Inserting a node with a duplicate id" should "return the invalid index but keep on creating" in {
//      val (client, project1) = SchemaDsl.schema().buildEmptyClientAndProject(isEjected = true)
//      setupProject(client, project1)
//
//      val types =
//        s"""type Model0 @model {
//           |  id: ID! @isUnique
//           |  a: Int!
//           |}""".stripMargin
//
//      val refreshedProject = setupProjectForTest(types, client, project1)
//
//      val nodes = """{"valueType":"nodes","values":[
//                  |{"_typeName": "Model0", "id": "just-some-id4", "a": 12},
//                  |{"_typeName": "Model0", "id": "just-some-id5", "a": 13},
//                  |{"_typeName": "Model0", "id": "just-some-id5", "a": 15}
//                  ]}""".stripMargin.parseJson
//
//      val importer = new BulkImport()
//      val res2     = importer.executeImport(refreshedProject, nodes).await(5)
//
//      res2.toString should (be(
//        """[{"index":2,"message":" Duplicate entry 'just-some-id5' for key 'PRIMARY'"},{"index":2,"message":" Duplicate entry 'just-some-id5' for key 'PRIMARY'"}]""")
//        or be(
//        """[{"index":1,"message":" Duplicate entry 'just-some-id5' for key 'PRIMARY'"},{"index":1,"message":" Duplicate entry 'just-some-id5' for key 'PRIMARY'"}]"""))
//
//      val res = executeQuerySimple("query{allModel0s{id, a}}", refreshedProject)
//      res.toString should (be("""{"data":{"allModel0s":[{"id":"just-some-id4","a":12},{"id":"just-some-id5","a":13}]}}""") or
//        be("""{"data":{"allModel0s":[{"id":"just-some-id4","a":12},{"id":"just-some-id5","a":15}]}}"""))
//    }
//
//    def setupProjectForTest(types: String, client: Client, project: Project): Project = {
//      val files  = Map("./types.graphql" -> types)
//      val config = newConfig(blankYamlWithGlobalStarPermission, files)
//      val push   = pushMutationString(config, project.id)
//      executeQuerySystem(push, client)
//      loadProjectFromDB(client.id, project.id)
//    }
}
