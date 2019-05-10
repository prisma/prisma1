package com.prisma.api.filters.nonEmbedded

import com.prisma.ConnectorTag.{DocumentConnectorTag, RelationalConnectorTag}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class SelfRelationFilterBugSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromStringV11() {
    connectorTag match {
      case _: RelationalConnectorTag =>
        """type Category {
          |  id: ID! @id
          |  name: String!
          |  parent: Category @relation(name: "C", link: INLINE)
          |}"""

      case _: DocumentConnectorTag =>
        """type Category {
          |  id: ID! @id
          |  name: String!
          |  parent: Category @relation(name: "C", link: INLINE)
          |}"""
    }
  }

  database.setup(project)
  val id = server
    .query("""mutation{createCategory(data:{name: "Sub", parent: {create:{ name: "Root"}} }){parent{id}}}""", project)
    .pathAsString("data.createCategory.parent.id")

  "Filter Queries along self relations" should "succeed with one level " in {
    val allCategories = s"""{
                       |  allCategories: categories {
                       |    name
                       |    parent {
                       |      name
                       |    }
                       |  }
                       |}"""

    val res1 = server.query(allCategories, project).toString
    res1 should be("""{"data":{"allCategories":[{"name":"Sub","parent":{"name":"Root"}},{"name":"Root","parent":null}]}}""")

    val rootCategories = s"""{
                       |  allRootCategories: categories(where: { parent: null }) {
                       |    name
                       |    parent {
                       |      name
                       |    }
                       |  }
                       |}"""

    val res2 = server.query(rootCategories, project).toString
    res2 should be("""{"data":{"allRootCategories":[{"name":"Root","parent":null}]}}""")

    val subCategories = s"""{
                           |  allSubCategories: categories(
                           |    where: {NOT:[{parent: null}] }
                           |  ) {
                           |    name
                           |    parent {
                           |      name
                           |    }
                           |  }
                           |}"""

    val res3 = server.query(subCategories, project).toString
    res3 should be("""{"data":{"allSubCategories":[{"name":"Sub","parent":{"name":"Root"}}]}}""")
  }

}
