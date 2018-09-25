package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.ApiConnectorCapability
import com.prisma.api.connector.ApiConnectorCapability.ScalarListsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class OrderByInMutationSpec extends FlatSpec with Matchers with ApiSpecBase {

  //Fixme switch this to MigrationCapability as soon as the unification of the Capabilities is merged
  override def runOnlyForCapabilities: Set[ApiConnectorCapability] = Set(ScalarListsCapability)

  val project = SchemaDsl.fromString() {
    """
      |type Foo {
      |    id: ID! @unique
      |    test: String
      |    bars: [Bar!]!
      |}
      |
      |type Bar {
      |    id: ID! @unique
      |    quantity: Int!
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  "The order when not giving an order by" should "be by Id ascending and therefore oldest first" in {
    val res = server.query(
      """mutation {
        |  createFoo(
        |    data: {
        |      bars: {
        |        create: [
        |          { quantity: 1 }
        |          { quantity: 2 }
        |        ]
        |      }
        |    }
        |  ) {
        |    test
        |    bars(first: 1, orderBy: createdAt_DESC) {
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
