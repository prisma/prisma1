package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class OptionalBackrelationSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "Nested Update" should "be generated models with missing backrelations " in {

    val project = SchemaDsl.fromString() {
      """
        |type Owner {
        |  id: ID!
        |  ownerName: String! @unique
        |  cat: Cat
        |}
        |
        |type Cat {
        |  id: ID!
        |  catName: String! @unique
        |}
        |
      """.stripMargin
    }
    database.setup(project)

    createItem(project, "Cat", "garfield")
    createItem(project, "Owner", "jon")

    //set initial owner
    val res = server.executeQuerySimple(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {connect: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    val res2 = server.executeQuerySimple(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {update: { where:{catName: "garfield"}, data: {catName: "azrael"}}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )
  }

  "Nested Upsert" should "be generated models with missing backrelations " in {

    val project = SchemaDsl.fromString() {
      """
        |type Owner {
        |  id: ID!
        |  ownerName: String! @unique
        |  cat: Cat
        |}
        |
        |type Cat {
        |  id: ID!
        |  catName: String! @unique
        |}
        |
      """.stripMargin
    }
    database.setup(project)

    createItem(project, "Cat", "garfield")
    createItem(project, "Owner", "jon")

    //set initial owner
    val res = server.executeQuerySimple(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {connect: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    val res2 = server.executeQuerySimple(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {upsert: {
        |                   where:{catName: "does not exist"},
        |                   update: {catName: "should not matter"}
        |                   create: {catName: "azrael"}
        |                   }}})
        |{
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )
  }

  "Nested Upsert" should "be generated for models with missing backrelations 2 " in {

    val project = SchemaDsl.fromString() {
      """
        |type Owner {
        |  id: ID!
        |  ownerName: String! @unique
        |  cat: Cat
        |}
        |
        |type Cat {
        |  id: ID!
        |  catName: String! @unique
        |}
        |
      """.stripMargin
    }
    database.setup(project)

    createItem(project, "Cat", "garfield")
    createItem(project, "Owner", "jon")

    //set initial owner
    val res = server.executeQuerySimple(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {connect: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    val res2 = server.executeQuerySimple(
      """mutation {updateOwner(where: {ownerName: "jon"},
        |data: {cat: {upsert: {
        |                   where:{catName: "garfield"},
        |                   update: {catName: "azrael"}
        |                   create: {catName: "should not matter"}
        |                   }}})
        |{
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateOwner":{"ownerName":"jon","cat":{"catName":"azrael"}}}}""")
  }

  def createItem(project: Project, modelName: String, name: String): Unit = {
    modelName match {
      case "Cat"   => server.executeQuerySimple(s"""mutation {createCat(data: {catName: "$name"}){id}}""", project)
      case "Owner" => server.executeQuerySimple(s"""mutation {createOwner(data: {ownerName: "$name"}){id}}""", project)
    }
  }

  def countItems(project: Project, name: String): Int = {
    server.executeQuerySimple(s"""query{$name{id}}""", project).pathAsSeq(s"data.$name").length
  }

}
