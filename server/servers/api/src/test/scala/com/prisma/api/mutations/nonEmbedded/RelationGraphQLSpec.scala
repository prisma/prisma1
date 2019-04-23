package com.prisma.api.mutations.nonEmbedded

import com.prisma.{IgnoreMongo, IgnoreSQLite}
import com.prisma.api.{ApiSpecBase, TestDataModels}
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class RelationGraphQLSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "One2One relations" should "only allow one item per side" in {
    val dms = {
      val dm1 = """type Owner{
                     id: ID! @id
                     ownerName: String @unique
                     cat: Cat @relation(link: INLINE)
                  }
                  
                  type Cat{
                     id: ID! @id
                     catName: String @unique
                     owner: Owner
                  }"""

      val dm2 = """type Owner{
                     id: ID! @id
                     ownerName: String @unique
                     cat: Cat
                  }
                  
                  type Cat{
                     id: ID! @id
                     catName: String @unique
                     owner: Owner @relation(link: INLINE)
                  }"""

      TestDataModels(mongo = Vector(dm1, dm2), sql = Vector(dm1, dm2))
    }
    dms.testV11 { project =>
      createItem(project, "Cat", "garfield")
      createItem(project, "Cat", "azrael")
      createItem(project, "Owner", "jon")
      createItem(project, "Owner", "gargamel")

      //set initial owner
      val res = server.query(
        """mutation { updateCat(
        |  where: {catName: "garfield"},
        |  data: {owner: {connect: {ownerName: "jon"}}}) {
        |    catName
        |    owner {
        |      ownerName
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      res.toString should be("""{"data":{"updateCat":{"catName":"garfield","owner":{"ownerName":"jon"}}}}""")

      val res2 = server.query("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
      res2.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":{"catName":"garfield"}}}}""")

      val res3 = server.query("""query{owner(where:{ownerName:"gargamel"}){ownerName, cat{catName}}}""", project)
      res3.toString should be("""{"data":{"owner":{"ownerName":"gargamel","cat":null}}}""")

      //change owner

      val res4 = server.query(
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

      val res5 = server.query("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
      res5.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":null}}}""")

      val res6 = server.query("""query{owner(where:{ownerName:"gargamel"}){ownerName, cat{catName}}}""", project)
      res6.toString should be("""{"data":{"owner":{"ownerName":"gargamel","cat":{"catName":"garfield"}}}}""")
    }
  }

  //Fixme this tests transactionality as well
  "Required One2One relations" should "throw an error if an update would leave one item without a partner" taggedAs (IgnoreMongo, IgnoreSQLite) in { // TODO: Remove when transactions are back
    val dms = {
      val dm1 = """type Owner{
                     id: ID! @id
                     ownerName: String @unique
                     cat: Cat! @relation(link: INLINE)
                  }

                  type Cat{
                     id: ID! @id
                     catName: String @unique
                     owner: Owner!
                  }"""

      val dm2 = """type Owner{
                     id: ID! @id
                     ownerName: String @unique
                     cat: Cat!
                  }

                  type Cat{
                     id: ID! @id
                     catName: String @unique
                     owner: Owner! @relation(link: INLINE)
                  }"""

      TestDataModels(mongo = Vector(dm1, dm2), sql = Vector(dm1, dm2))
    }
    dms.testV11 { project =>
      //set initial owner
      val res = server.query(
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

      val res2 = server.query("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
      res2.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":{"catName":"garfield"}}}}""")

      //create new owner and connect to garfield

      server.queryThatMustFail(
        """mutation {createOwner(
        |data: {ownerName: "gargamel", cat : {connect: {catName: "garfield"}}}) {
        |    ownerName
        |    cat {
        |      catName
        |    }
        |  }
        |}""",
        project,
        errorCode = 3042,
        errorContains = "The change you are trying to make would violate the required relation 'CatToOwner' between Cat and Owner"
      )

      val res5 = server.query("""query{owner(where:{ownerName:"jon"}){ownerName, cat{catName}}}""", project)
      res5.toString should be("""{"data":{"owner":{"ownerName":"jon","cat":{"catName":"garfield"}}}}""")

      val res6 = server.query("""query{owner(where:{ownerName:"gargamel"}){ownerName, cat{catName}}}""", project)
      res6.toString should be("""{"data":{"owner":null}}""")
    }
  }

  def createItem(project: Project, modelName: String, name: String): Unit = {
    modelName match {
      case "Cat"   => server.query(s"""mutation {createCat(data: {catName: "$name"}){id}}""", project)
      case "Owner" => server.query(s"""mutation {createOwner(data: {ownerName: "$name"}){id}}""", project)
    }
  }

  def countItems(project: Project, name: String): Int = {
    server.query(s"""query{$name{id}}""", project).pathAsSeq(s"data.$name").length
  }

}
