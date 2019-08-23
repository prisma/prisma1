package writes

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.ScalarListsCapability
import util._

class DeleteScalarListsSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability)

  "A toplevel delete  mutation" should "also delete ListTable entries" in {
    val testDataModels = {
      val dm1 = """
        |model TestModel {
        | id   String @id @default(cuid())
        | name String @unique
        | list Int[]
        |}"""


      val dm2 = """
        |model TestModel {
        | id   String @id @default(cuid())
        | name String @unique
        | list Int[] // TODO: must readd @scalarList(strategy: RELATION)
        |}"""

      TestDataModels(mongo = dm1, sql = dm2)
    }

    testDataModels.testV11 { project =>
      server.query(
        """mutation {
        |  createTestModel(
        |    data: { name: "test", list: {set: [1,2,3]} }
        |  ){
        |    name
        |    list
        |  }
        |}
      """,
        project
      )

      server.query("""mutation{deleteTestModel(where:{name:"test" }){name}}""", project)

      server.query("""query{testModels{name}}""", project).toString() should be("""{"data":{"testModels":[]}}""")
    }
  }

  "A delete Many  mutation" should "also delete ListTable entries" in {
    val testDataModels = {
      val dm1 = """model Top {
                   id   String @id @default(cuid())
                   name String @unique
                   list Int[]
                  }"""

      val dm2 = """model Top {
                   id   String @id @default(cuid())
                   name String @unique
                   list Int[]  // @scalarList(strategy: RELATION)
                  }"""

      TestDataModels(mongo = dm1, sql = dm2)
    }

    testDataModels.testV11 { project =>
      server.query("""mutation {createTop(data: { name: "test", list: {set: [1,2,3]}}){name, list}}""", project)
      server.query("""mutation {createTop(data: { name: "test2", list: {set: [1,2,3]}}){name, list}}""", project)
      server.query("""mutation {createTop(data: { name: "test3", list: {set: [1,2,3]}}){name, list}}""", project)

      val res = server.query("""mutation{deleteManyTops(where:{name_contains:"2" }){count}}""", project)

      res.toString should be("""{"data":{"deleteManyTops":{"count":1}}}""")
    }

  }
}
