package com.prisma.api.mutations.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.ApiConnectorCapability.JoinRelationsCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class VeryManyMutationsSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationsCapability)
  val project: Project = SchemaDsl.fromString() {
    """
      |type Top {
      |   id: ID! @unique
      |   int: Int!
      |   middles:[Middle!]!
      |}
      |
      |type Middle {
      |   id: ID! @unique
      |   int: Int!
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  //Postgres has a limit of 32678 parameters to a query

  "The delete many Mutation" should "delete the items matching the where clause" in {
    createVeryManyTops

    val update = server.query("""mutation {updateManyMiddles(where: { int_gt: 100 } data:{int: 500}){count}}""", project)
    update.pathAsLong("data.updateManyMiddles.count") should equal(36291)

    val result = server.query("""mutation {deleteManyMiddles(where: { int_gt: 100 }){count}}""", project)
    result.pathAsLong("data.deleteManyMiddles.count") should equal(36291)
  }

  "A cascading delete" should "not hit the parameter limit" in {}

  def createVeryManyTops = for (int <- 1 to 1000) {
    createTop(int)
  }

  def createTop(int: Int): Unit = {
    val query =
      s"""mutation a {createTop(data: {
         |  int: $int
         |  middles: {create: [
         |  {int: ${int}1},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: ${int}20},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: $int},
         |  {int: ${int}40}
         |  ]}
         |}) {int}}"""

    server.query(query, project)
  }
}
