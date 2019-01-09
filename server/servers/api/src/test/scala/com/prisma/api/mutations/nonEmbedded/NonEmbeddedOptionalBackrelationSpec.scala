package com.prisma.api.mutations.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NonEmbeddedOptionalBackrelationSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "Nested Updates" should "work for models with missing backrelations " in {
    val project = SchemaDsl.fromString() {
      """
        |type Owner {
        |  id: ID! @unique
        |  ownerName: String! @unique
        |  cat: Cat
        |}
        |
        |type Cat {
        |  id: ID! @unique
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
    val project = SchemaDsl.fromString() {
      """
        |type Owner {
        |  id: ID! @unique
        |  ownerName: String! @unique
        |  cats: [Cat]
        |}
        |
        |type Cat {
        |  id: ID! @unique
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
    val project = SchemaDsl.fromString() {
      """
        |type Owner {
        |  id: ID! @unique
        |  ownerName: String! @unique
        |  cats: [Cat]
        |}
        |
        |type Cat {
        |  id: ID! @unique
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
