package queries

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class OrderByInMutationSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  val testDataModels = {
    val s1 = """
      model Foo {
          id   String  @id @default(cuid())
          test String?
          bars Bar[]   @relation(references: [id])
      }
      
      model Bar {
          id         String @id @default(cuid())
          quantity   Int
          orderField Int?
      }
    """

    val s2 = """
      model Foo {
          id   String  @id @default(cuid())
          test String?
          bars Bar[]
      }
      
      model Bar {
          id         String @id @default(cuid())
          quantity   Int
          orderField Int?
      }
    """

    TestDataModels(mongo = Vector(s1), sql = Vector(s2))
  }

  "Using a field in the order by that is not part of the selected fields" should "work" in {
    testDataModels.testV11 { project =>
      val res = server.query(
        """mutation {
        |  createFoo(
        |    data: {
        |      bars: {
        |        create: [
        |          { quantity: 1, orderField: 1}
        |          { quantity: 2, orderField: 2}
        |        ]
        |      }
        |    }
        |  ) {
        |    test
        |    bars(first: 1, orderBy: orderField_DESC) {
        |      quantity
        |    }
        |  }
        |}
      """,
        project
      )

      res.toString should be("""{"data":{"createFoo":{"test":null,"bars":[{"quantity":2}]}}}""")
    }

  }

}
