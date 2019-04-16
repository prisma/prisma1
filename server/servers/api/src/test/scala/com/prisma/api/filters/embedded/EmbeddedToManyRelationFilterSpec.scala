package com.prisma.api.filters.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedToManyRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Using a toMany relational filter with _some" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded{
        |   unique: Int!
        |   name: String!
        |   bottom: [Bottom]
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
         |   middle: {create:[{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:[{
         |          unique: 111,
         |          name: "Bottom"
         |      }]}
         |   },
         |   {
         |      unique: 12,
         |      name: "Middle"
         |      bottom: {create:[{
         |          unique: 112,
         |          name: "Bottom"
         |      }]}
         |   }]}
         |}){
         |  unique
         |}}""",
      project
    )

    res1.toString should be("""{"data":{"createTop":{"unique":1}}}""")

    val query = server.query(s"""query { tops(where:{middle_some:{bottom_some:{unique: 111, name:"Bottom"}}}){unique}}""", project)
    query.toString should be("""{"data":{"tops":[{"unique":1}]}}""")

    val query2 = server.query(s"""query { tops(where:{middle_none:{bottom_some:{unique: 111, name:"Bottom"}}}){unique}}""", project)
    query2.toString should be("""{"data":{"tops":[]}}""")

    val query3 = server.query(s"""query { tops(where:{middle_every:{bottom_some:{unique: 111, name:"Bottom"}}}){unique}}""", project)
    query3.toString should be("""{"data":{"tops":[]}}""")

    val query4 = server.query(s"""query { tops(where:{middle_some:{bottom_every:{unique: 111, name:"Bottom"}}}){unique}}""", project)
    query4.toString should be("""{"data":{"tops":[{"unique":1}]}}""")

  }

  "Using a toMany relational filter with _every" should "work 2" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int!
        |   name: String!
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded{
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
         |   middle: {create:[
         |   {
         |      unique: 11,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 12,
         |      name: "Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res1.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11},{"unique":12}]}}}""")

    val res2 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 2,
         |   name: "Top",
         |   middle: {create:[
         |   {
         |      unique: 21,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 22,
         |      name: "Not-Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res2.toString should be("""{"data":{"createTop":{"unique":2,"middle":[{"unique":21},{"unique":22}]}}}""")

    val query = server.query(
      s"""query { tops(where:{middle_every:{name:"Middle"}})
         |{
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    query.toString should be("""{"data":{"tops":[{"unique":1,"middle":[{"unique":11},{"unique":12}]}]}}""")
  }

  "Using a toMany relational filter with _none" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int!
        |   name: String!
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded{
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
         |   middle: {create:[
         |   {
         |      unique: 11,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 12,
         |      name: "Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res1.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11},{"unique":12}]}}}""")

    val res2 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 2,
         |   name: "Top",
         |   middle: {create:[
         |   {
         |      unique: 21,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 22,
         |      name: "Not-Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res2.toString should be("""{"data":{"createTop":{"unique":2,"middle":[{"unique":21},{"unique":22}]}}}""")

    val res3 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 3,
         |   name: "Top",
         |   middle: {create:[
         |   {
         |      unique: 31,
         |      name: "Not-Middle"
         |   },
         |   {
         |      unique: 32,
         |      name: "Not-Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res3.toString should be("""{"data":{"createTop":{"unique":3,"middle":[{"unique":31},{"unique":32}]}}}""")

    val query = server.query(
      s"""query { tops(where:{middle_none:{name:"Middle"}})
         |{
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    query.toString should be("""{"data":{"tops":[{"unique":3,"middle":[{"unique":31},{"unique":32}]}]}}""")
  }

  "Using a toMany relational filter with _every" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """type Top {
        |   id: ID! @id
        |   unique: Int!
        |   name: String!
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded{
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
         |   middle: {create:[
         |   {
         |      unique: 11,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 12,
         |      name: "Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res1.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11},{"unique":12}]}}}""")

    val res2 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 2,
         |   name: "Top",
         |   middle: {create:[
         |   {
         |      unique: 21,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 22,
         |      name: "Not-Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res2.toString should be("""{"data":{"createTop":{"unique":2,"middle":[{"unique":21},{"unique":22}]}}}""")

    val res3 = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 3,
         |   name: "Top",
         |   middle: {create:[
         |   {
         |      unique: 31,
         |      name: "Not-Middle"
         |   },
         |   {
         |      unique: 32,
         |      name: "Not-Middle"
         |   }
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    res3.toString should be("""{"data":{"createTop":{"unique":3,"middle":[{"unique":31},{"unique":32}]}}}""")

    val query = server.query(
      s"""query { tops(where:{middle_every:{name:"Middle"}})
         |{
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""",
      project
    )

    query.toString should be("""{"data":{"tops":[{"unique":1,"middle":[{"unique":11},{"unique":12}]}]}}""")
  }
}
