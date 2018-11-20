package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class OrderByInMutationSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromString() {
    """
      |type Foo {
      |    id: ID! @unique
      |    test: String
      |    bars: [Bar]
      |}
      |
      |type Bar {
      |    id: ID! @unique
      |    quantity: Int!
      |    orderField: Int
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  "Using a field in the order by that is not part of the selected fields" should "work" in {
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
