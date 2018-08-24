package com.prisma.api.filters.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedRelationIsNullFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def onlyRunSuiteForMongo: Boolean = true

  "Using a toOne relational filter" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |}
        |
        |type Middle @embedded{
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":null}}}""")

    val res3 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 2,
         |   name: "Top",
         |   middle: {create:{
         |      unique: 11,
         |      name: "Middle"
         |   }
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res3.toString should be("""{"data":{"createTop":{"unique":2,"middle":{"unique":11}}}}""")

    val res2 = server.query(
      s"""query { tops(where:{middle:null})
         |{
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"tops":[{"unique":1,"middle":null}]}}""")
  }
}
