package com.prisma.api.mutations.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, RelationLinkListCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NonEmbeddedOptionalBackrelationSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val inlineDirective = if (capabilities.has(RelationLinkListCapability)) {
    "@relation(link: INLINE)"
  } else {
    ""
  }

  "Nested Updates" should "work for models with missing backrelations " in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Owner {
        |  id: ID! @id
        |  ownerName: String! @unique
        |  cat: Cat @relation(link: INLINE)
        |}
        |
        |type Cat {
        |  id: ID! @id
        |  catName: String! @unique
        |}
        |
      """.stripMargin
    }
    database.setup(project)

    server.query(
      """mutation {createOwner(data: {ownerName: "jon", cat: {create: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    val res = server.query(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {update:{catName: "azrael"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res.toString() should be("""{"data":{"updateOwner":{"ownerName":"jon","cat":{"catName":"azrael"}}}}""")
  }

  "Nested Upsert" should "work for models with missing backrelations for update " in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Owner {
        |  id: ID! @id
        |  ownerName: String! @unique
        |  cats: [Cat] $inlineDirective
        |}
        |
        |type Cat {
        |  id: ID! @id
        |  catName: String! @unique
        |}
        |
      """.stripMargin
    }
    database.setup(project)

    server.query(
      """mutation {createOwner(data: {ownerName: "jon", cats: {create: {catName: "garfield"}}}) {
        |    ownerName
        |    cats {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    val res = server.query(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cats: {upsert: {
        |                   where:{catName: "garfield"},
        |                   update: {catName: "azrael"}
        |                   create: {catName: "should not matter"}
        |                   }}})
        |{
        |    ownerName
        |    cats {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateOwner":{"ownerName":"jon","cats":[{"catName":"azrael"}]}}}""")
  }

  "Nested Upsert" should "work for models with missing backrelations for create" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Owner {
        |  id: ID! @id
        |  ownerName: String! @unique
        |  cats: [Cat] $inlineDirective
        |}
        |
        |type Cat {
        |  id: ID! @id
        |  catName: String! @unique
        |}
        |
      """.stripMargin
    }
    database.setup(project)

    server.query(
      """mutation {createOwner(data: {ownerName: "jon", cats: {create: {catName: "garfield"}}}) {
        |    ownerName
        |    cats {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    val res = server.query(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cats: {upsert: {
        |                   where:{catName: "DOES NOT EXIST"},
        |                   update: {catName: "SHOULD NOT MATTER"}
        |                   create: {catName: "azrael"}
        |                   }}})
        |{
        |    ownerName
        |    cats {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateOwner":{"ownerName":"jon","cats":[{"catName":"garfield"},{"catName":"azrael"}]}}}""")
  }
}
