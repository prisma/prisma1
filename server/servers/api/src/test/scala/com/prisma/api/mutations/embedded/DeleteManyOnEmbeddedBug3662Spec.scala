package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteManyOnEmbeddedBug3662Spec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  val schema =
    """type Item {
      |  id: ID! @unique
      |  name: String @unique
      |  subItems: [SubItem!]!
      |}
      |
      |type SubItem @embedded {
      |  id: ID! @unique
      |  name: String @unique
      |}"""

  lazy val project: Project = SchemaDsl.fromString() {
    schema
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "The delete many Mutation" should "delete the items matching the where relation filter when using in" in {

    server.query(
      s"""mutation {
         |  createItem(
         |    data: { name: "ITEM", subItems: { create: [{ name: "TEST1" }, { name: "TEST2" }] } }
         |  ) {
         |    id
         |    subItems {
         |      name
         |      id
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    val res1 = server.query(
      s"""mutation {
         |  updateItem(
         |    data: {
         |      subItems: {
         |        deleteMany: {
         |          name_in: ["TEST1", "TEST2"]
         |        }
         |      }
         |    }
         |    where: { name: "ITEM" }
         |  ) {
         |    name
         |    subItems {
         |      name
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res1.toString() should be("""{"data":{"updateItem":{"name":"ITEM","subItems":[]}}}""")

    val res2 = server.query("query{items{name, subItems{name}}}", project)

    res2.toString() should be("""{"data":{"items":[{"name":"ITEM","subItems":[]}]}}""")

  }
}
