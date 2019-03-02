package com.prisma.api.filters.nonEmbedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class FilterUnwrapSomeSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project: Project = SchemaDsl.fromString() { """type Item {
                                                    |  id: ID! @unique
                                                    |  name: String @unique
                                                    |  subItems: [SubItem!]!
                                                    |}
                                                    |
                                                    |type SubItem {
                                                    |  id: ID! @unique
                                                    |  name: String @unique
                                                    |}""".stripMargin }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    populate
  }

  "Many filter" should "work" in {

    val query = """mutation {
                  |  updateItem(
                  |    data: {
                  |      subItems: {
                  |        deleteMany: {
                  |          name_in: ["TEST1", "TEST2"]
                  |        }
                  |      }
                  |    }
                  |    where: { name: "Top" }
                  |  ) {
                  |    name
                  |    subItems {
                  |      name
                  |    }
                  |  }
                  |}"""

    server.query(query, project)
  }

  def populate: Unit = {
    server.query(
      s"""mutation {
         |  createItem(
         |    data: { name: "Top", subItems: { create: [{ name: "TEST1" }, { name: "TEST2" }] } }
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
  }
}
