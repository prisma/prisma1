package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ApiConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoPrototypingSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

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

  "Create in Update" should "add to toMany relations" in {

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
        | }"""
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
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11},{"unique":12}]}}}""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      middle: {create:[{
         |          unique: 13,
         |          name: "Middle3"
         |          },
         |          {
         |          unique: 14,
         |          name: "Middle4"
         |        }]
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique,
         |  }
         |}}""",
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":[{"unique":11},{"unique":12},{"unique":13},{"unique":14}]}}}""")

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
         |          name: "MiddleNew"
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

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":{"unique":11,"name":"MiddleNew"}}}}""")
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
         |   middle: {create:[{
         |      unique: 11,
         |      name: "Middle"
         |   },
         |   {
         |      unique: 12,
         |      name: "Middle2"
         |   }
         |
         |   ]}
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11},{"unique":12}]}}}""")

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

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":[{"unique":12}]}}}""")
  }

  "To many and toOne mixedrelations deleting over two levels" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
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
         |   middle: {create:{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:[
         |        {
         |          unique: 111,
         |          name: "Bottom"
         |        },
         |        {
         |          unique: 112,
         |          name: "Bottom"
         |        }
         |      ]
         |      }}
         |    }
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

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11,"bottom":[{"unique":111},{"unique":112}]}}}}""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {update:
         |      {bottom: {delete:{unique:111}} }}
         |}){
         |  unique,
         |  middle{
         |    unique
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":{"unique":11,"bottom":[{"unique":112}]}}}}""")
  }

  "To many and toOne mixedrelations deleting over two levels" should "error correctly" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
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
         |   middle: {create:{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:[
         |        {
         |          unique: 111,
         |          name: "Bottom"
         |        },
         |        {
         |          unique: 112,
         |          name: "Bottom"
         |        }
         |      ]
         |      }}
         |    }
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

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11,"bottom":[{"unique":111},{"unique":112}]}}}}""")

    server.queryThatMustFail(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {update:
         |      {bottom: {delete:{unique:113}} }}
         |}){
         |  unique,
         |  middle{
         |    unique
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""".stripMargin,
      project,
      errorCode = 3041,
      errorContains =
        """The relation BottomToMiddle has no node for the model Middle connected to a Node for the model Bottom with the value '113' for the field 'unique'"""
    )
  }

  "To many relations deleting over two levels" should "work" in {

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

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {update:{
         |                where:{unique:11}
         |                data: {
         |                  name: "MiddleNew"
         |                  bottom: {delete:{unique:111}} }}
         |                  }
         |}){
         |  unique,
         |  middle{
         |    unique,
         |    name,
         |    bottom{
         |      unique,
         |    }
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be(
      """{"data":{"updateTop":{"unique":1,"middle":[{"unique":11,"name":"MiddleNew","bottom":[]},{"unique":12,"name":"Middle2","bottom":[{"unique":112}]}]}}}""")
  }

  "Creating nested inline relations" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |}
        |
        |type Middle {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   top: Top
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createMiddle(data: {
         |   unique: 11111,
         |   name: "MiddleNonEmbeddedReversed",
         |   top: {create:{
         |      unique: 111111,
         |      name: "TopNonEmbeddedReversed"
         |    }
         |   }
         |}){
         |  unique,
         |  top{
         |    unique
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createMiddle":{"unique":11111,"top":{"unique":111111}}}}""")

    server.query("query{middles{unique, top {unique}}}", project).toString should be("""{"data":{"middles":[{"unique":11111,"top":{"unique":111111}}]}}""")
    server.query("query{tops{unique, middle {unique}}}", project).toString should be("""{"data":{"tops":[{"unique":111111,"middle":{"unique":11111}}]}}""")
  }

  "Simple unique index" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 11111,
         |   name: "Top"
         |}){
         |  unique,
         |  name
         |}}""",
      project
    )

    server.queryThatMustFail(
      s"""mutation {
         |   createTop(data: {
         |   unique: 11111,
         |   name: "Top"
         |}){
         |  unique,
         |  name
         |}}""",
      project,
      3010,
      errorContains = """A unique constraint would be violated on Top. Details: Field name = unique"""
    )
  }

  "Relations from embedded to Non-Embedded" should "work 1" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    name: String
        |    child: Child
        |}
        |
        |type Friend{
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friend: Friend @mongoRelation(field: "friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{
         |      name: "Daughter"
         |      friend: {create:{
         |          name: "Buddy"
         |      }
         |      }
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter","friend":{"name":"Buddy"}}}}}""")
  }

  "Relations from embedded to Non-Embedded" should "work 2" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    name: String @unique
        |    children: [Child!]!
        |}
        |
        |type Friend{
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String @unique
        |    friend: Friend @mongoRelation(field: "friend")
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:{
         |      name: "Daughter"
         |   }}
         |}){
         |  name,
         |  children{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    val res = server.query(
      s"""mutation {
         |   updateParent(
         |   where:{name: "Dad"}
         |   data: {
         |   children: {update:{
         |      where: {name: "Daughter"}
         |      data: {
         |          friend:{create:{name: "Buddy"}}
         |      }
         |   }}
         |}){
         |  name,
         |  children{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter","friend":{"name":"Buddy"}}]}}}""")
  }

  "Relations from embedded to Non-Embedded" should "work 3" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    name: String
        |    children: [Child!]!
        |}
        |
        |type Friend{
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friend: Friend @mongoRelation(field: "friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:{
         |      name: "Daughter"
         |      friend: {create:{
         |          name: "Buddy"
         |      }
         |      }
         |   }}
         |}){
         |  name,
         |  children{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter","friend":{"name":"Buddy"}}]}}}""")
  }

  "Relations from embedded to Non-Embedded" should "work 4" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    name: String
        |    children: [Child!]!
        |}
        |
        |type Friend{
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friends: [Friend!]! @mongoRelation(field: "friends")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[
         |   {name: "Daughter", friends: {create:[{name: "Buddy"},{name: "Buddy2"}]}},
         |   {name: "Daughter2", friends: {create:[{name: "Buddy3"},{name: "Buddy4"}]}}
         |   ]}
         |}){
         |  name,
         |  children{
         |    name
         |    friends{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter","friends":[{"name":"Buddy"},{"name":"Buddy2"}]},{"name":"Daughter2","friends":[{"name":"Buddy3"},{"name":"Buddy4"}]}]}}}""")
  }
}
