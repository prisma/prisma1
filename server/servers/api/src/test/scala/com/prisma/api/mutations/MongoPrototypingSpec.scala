package com.prisma.api.mutations

import com.prisma.IgnorePassive
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoPrototypingSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def onlyRunSuiteForMongo: Boolean = true

  "To one relations" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |   createdAt: DateTime!
        |}
        |
        |type Middle @embedded{
        |   unique: Int! @unique
        |   name: String!
        |   bottom: Bottom
        |   createdAt: DateTime!
        |}
        |
        |type Bottom @embedded{
        |   unique: Int! @unique
        |   name: String!
        |   updatedAt: DateTime!
        |}"""
    }

    database.setup(project)

    val res = server.query(
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
         |    unique,
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11,"bottom":{"unique":111}}}}}""")
  }

  "To many relations" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle!]!
        |}
        |
        |type Middle @embedded {
        |   unique: Int! @unique
        |   name: String!
        |   bottom: [Bottom!]!
        |}
        |
        |type Bottom @embedded{
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
         |   middle: {create:[{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:{
         |          unique: 111,
         |          name: "Bottom"
         |      }}},
         |      {
         |      unique: 12,
         |      name: "Middle2"
         |      bottom: {create:{
         |          unique: 112,
         |          name: "Bottom2"
         |      }}
         |    }]
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique,
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11,"bottom":[{"unique":111}]},{"unique":12,"bottom":[{"unique":112}]}]}}}""")
  }

  "ListValues" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   ints: [Int!]!
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |   ints: {set:[1,2,3,4,5]}
         |}){
         |  unique,
         |  ints
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"ints":[1,2,3,4,5]}}}""")
  }

  "Update with nested Create" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle!]!
        |}
        |
        |type Middle @embedded {
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top"
         |}){
         |  unique
         |}}""".stripMargin,
      project
    )

    val res = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique:1}
         |   data: {
         |   name: "Top2",
         |   middle: {create:[
         |      {
         |      unique: 11,
         |      name: "Middle"
         |      },
         |      {
         |      unique: 12,
         |      name: "Middle2"
         |    }]
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateTop":{"unique":1,"middle":[{"unique":11},{"unique":12}]}}}""")
  }

  "Updating toOne relations" should "work" in {

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

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11}}}}""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {update:{
         |          name: "Middle2"
         |      }
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique
         |    name
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":{"unique":11,"name":"Middle2"}}}}""")
  }

  "Deleting toOne relations" should "work" in {

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

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11}}}}""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {delete: true}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":null}}}""")
  }

  "Deleting toMany relations if they have a unique" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle!]!
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

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11}]}}}""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {delete:{unique:11}}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":null}}}""")
  }

  "Finding an item by where" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}
        |"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top"
         |}){
         |  unique,
         |}}""",
      project
    )

    val res2 = server.query(
      s"""mutation {
         |   deleteTop(
         |   where:{unique: 1}
         |   ){
         |  unique,
         |}}""",
      project
    )
  }

  "Using a toMany relational filter with _every" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle!]!
        |}
        |
        |type Middle @embedded{
        |   unique: Int! @unique
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

    query.toString should be("""{"data":{"tops":[{"unique":1,"middle":{"unique":11}}]}}""")
  }

}
