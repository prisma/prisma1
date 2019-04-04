package com.prisma.api.filters.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedToOneRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Using a toOne relational filter checking for null " should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |}
        |
        |type Middle @embedded{
        |   unique: Int!
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

  "Using a toOne relational filter checking for two nested values" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |}
        |
        |type Middle @embedded{
        |   unique: Int!
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
      s"""query { tops(where:{middle:{unique: 11, name:"Middle"}})
         |{
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"tops":[{"unique":2,"middle":{"unique":11}}]}}""")
  }

  "Using a toOne relational filter over two levels" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |}
        |
        |type Middle @embedded{
        |   unique: Int!
        |   name: String!
        |   bottom: Bottom
        |}
        |
        |type Bottom @embedded{
        |   unique: Int!
        |   name: String!
        |}"""
    }

    database.setup(project)

    val res1 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |   middle: {create:{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:{
         |          unique: 111,
         |          name: "Bottom"
         |      }}
         |   }}
         |}){
         |  unique,
         |  middle{
         |    unique
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""",
      project
    )

    res1.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11,"bottom":{"unique":111}}}}}""")

    val res2 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 2,
         |   name: "Top",
         |   middle: {create:{
         |      unique: 22,
         |      name: "Middle"
         |      bottom: {create:{
         |          unique: 222,
         |          name: "Bottom"
         |      }}
         |   }}
         |}){
         |  unique,
         |  middle{
         |    unique
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""",
      project
    )

    res2.toString should be("""{"data":{"createTop":{"unique":2,"middle":{"unique":22,"bottom":{"unique":222}}}}}""")

    val query = server.query(
      s"""query { tops(where:{middle:{bottom:{unique: 111, name:"Bottom"}}})
         |{
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    query.toString should be("""{"data":{"tops":[{"unique":1,"middle":{"unique":11}}]}}""")
  }
}
