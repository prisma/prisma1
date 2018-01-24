package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models.Project
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class RelationGraphQLSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "One2One relations" should "only allow one item per side" in {

    val project = SchemaDsl() { schema =>
      val cat = schema.model("Cat").field("catName", _.String, isUnique = true)
      schema.model("Owner").field("ownerName", _.String, isUnique = true).oneToOneRelation("cat", "owner", cat)
    }

    database.setup(project)

    createItem(project, "Cat", "garfield")
    createItem(project, "Cat", "azrael")
    createItem(project, "Owner", "jon")
    createItem(project, "Owner", "gargamel")

    //set initial owner
    val res = server.executeQuerySimple(
      """mutation {updateCat(where: {catName: "garfield"},
        |data: {owner: {connect: {ownerName: "jon"}}}) {
        |    catName
        |    owner {
        |      ownerName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateCat":{"catName":"garfield","owner":{"ownerName":"jon"}}}}""")

    val res2 = server.executeQuerySimple("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
    res2.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":{"catName":"garfield"}}}}""")

    val res3 = server.executeQuerySimple("""query{owner(where:{ownerName:"gargamel"}){ownerName, cat{catName}}}""", project)
    res3.toString should be("""{"data":{"owner":{"ownerName":"gargamel","cat":null}}}""")

    //change owner

    val res4 = server.executeQuerySimple(
      """mutation {updateCat(where: {catName: "garfield"},
        |data: {owner: {connect: {ownerName: "gargamel"}}}) {
        |    catName
        |    owner {
        |      ownerName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res4.toString should be("""{"data":{"updateCat":{"catName":"garfield","owner":{"ownerName":"gargamel"}}}}""")

    val res5 = server.executeQuerySimple("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
    res5.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":null}}}""")

    val res6 = server.executeQuerySimple("""query{owner(where:{ownerName:"gargamel"}){ownerName, cat{catName}}}""", project)
    res6.toString should be("""{"data":{"owner":{"ownerName":"gargamel","cat":{"catName":"garfield"}}}}""")
  }

  "Required One2One relations" should "throw an error if an update would leave one item without a partner" in {

    val project = SchemaDsl() { schema =>
      val cat = schema.model("Cat").field("catName", _.String, isUnique = true)
      schema.model("Owner").field("ownerName", _.String, isUnique = true).oneToOneRelation_!("cat", "owner", cat)
    }

    database.setup(project)

    //set initial owner
    val res = server.executeQuerySimple(
      """mutation {createOwner(
        |data: {ownerName: "jon", cat : {create: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createOwner":{"ownerName":"jon","cat":{"catName":"garfield"}}}}""")

    val res2 = server.executeQuerySimple("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
    res2.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":{"catName":"garfield"}}}}""")

    //create new owner and connect to garfield

    val res3 = server.executeQuerySimple(
      """mutation {createOwner(
        |data: {ownerName: "gargamel", cat : {connect: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    res3.toString should be("""{"data":{"createOwner":{"ownerName":"gargamel","cat":{"catName":"garfield"}}}}""")

//    val res5 = server.executeQuerySimple("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
//    res5.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":null}}}""")
//
//    val res6 = server.executeQuerySimple("""query{owner(where:{ownerName:"gargamel"}){ownerName, cat{catName}}}""", project)
//    res6.toString should be("""{"data":{"owner":{"ownerName":"gargamel","cat":{"catName":"garfield"}}}}""")
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
