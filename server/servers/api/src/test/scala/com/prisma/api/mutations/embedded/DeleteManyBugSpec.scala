package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteManyBugSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  val schema =
    """type User {
      |  id: ID! @id
      |  name: String! @unique
      |  balances: [Balance!]!
      |}
      |
      |type Balance @embedded {
      |    currency: String!
      |    platform: String!
      |    amount: Float!
      |}"""

  lazy val project: Project = SchemaDsl.fromStringV11() { schema }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "The delete many Mutation" should "delete the items matching the where relation filter" in {

    server.query(
      s"""mutation {
         |  createUser(
         |    data: {
         |      name: "user"
         |      balances: {
         |        create: [
         |          {platform: "OnlineWallet", currency: "BTC", amount: 1}
         |          {platform: "PaperWallet", currency: "BTC", amount: 1}
         |          {platform: "OnlineWallet", currency: "ETH", amount: 1}
         |        ]
         |      }
         |    }
         |  ) {
         |    id
         |  }
         |}
      """,
      project
    )

    val res = server.query(
      s"""mutation {
         |  updateUser(
         |    data: { balances: { deleteMany: { platform: "PaperWallet", currency: "BTC" } } }
         |    where: { name: "user" }
         |  ) {
         |    name
         |    balances {
         |      platform
         |      currency
         |    }
         |  }
         |}
      """,
      project
    )

    res.toString should be(
      """{"data":{"updateUser":{"name":"user","balances":[{"platform":"OnlineWallet","currency":"BTC"},{"platform":"OnlineWallet","currency":"ETH"}]}}}""")

  }

}
