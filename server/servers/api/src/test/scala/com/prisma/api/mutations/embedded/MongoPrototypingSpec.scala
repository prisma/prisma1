package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
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
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded {
        |   unique: Int! @unique
        |   name: String!
        |   bottom: [Bottom]
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
        |   middle: [Middle]
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
        |   ints: [Int]
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
        |   middle: [Middle]
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
        |   middle: [Middle]
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
        |   bottom: [Bottom]
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
        |   bottom: [Bottom]
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
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded {
        |   unique: Int! @unique
        |   name: String!
        |   bottom: [Bottom]
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
        |    children: [Child]
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
        |    children: [Child]
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
        |    children: [Child]
        |}
        |
        |type Friend{
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friends: [Friend] @mongoRelation(field: "friends")
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

  "Delete" should "take care of relations without foreign keys" in {

    val project = SchemaDsl.fromString() {
      """
        |type Child {
        |    id: ID! @unique
        |    name: String @unique
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: Child @mongoRelation(field: "children")
        |}
        |"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{name: "Daughter"}}
         |}){
         |  name,
         |  child{
         |    name
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter"}}}}""")

    server.query(s"""mutation {deleteChild(where:{name: "Daughter"}){name}}""", project)

    server.query(s"""query {parents{name, child {name}}}""", project)

  }

  "Delete of something linked to on an embedded type" should "take care of relations without foreign keys " in {

    val project = SchemaDsl.fromString() {
      """
        |type Friend {
        |    name: String @unique
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: Child
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |    child: GrandChild
        |}
        |
        |type GrandChild @embedded{
        |    name: String @unique
        |    friend: Friend @mongoRelation(field:"friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{
         |   name: "Daughter"
         |   child: {create:{
         |      name: "GrandSon"
         |      friend: {create:{
         |          name: "Friend"
         |          }}
         |      }}
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    child{
         |       name
         |       friend{
         |          name
         |       }
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter","child":{"name":"GrandSon","friend":{"name":"Friend"}}}}}}""")

    server.query(s"""mutation {deleteFriend(where:{name: "Friend"}){name}}""", project)

    server.query(s"""query {parents{name, child {name}}}""", project)
  }

  "Delete of something linked to on an embedded type" should "take care of relations without foreign keys 2" in {

    val project = SchemaDsl.fromString() {
      """
        |type Friend {
        |    name: String @unique
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: Child
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |    child: GrandChild
        |}
        |
        |type GrandChild @embedded{
        |    name: String @unique
        |    friend: Friend! @mongoRelation(field:"friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{
         |   name: "Daughter"
         |   child: {create:{
         |      name: "GrandSon"
         |      friend: {create:{
         |          name: "Friend"
         |          }}
         |      }}
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    child{
         |       name
         |       friend{
         |          name
         |       }
         |    }
         |  }
         |}}""",
      project
    )

    val res2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   child: {create:{
         |   name: "Daughter2"
         |   child: {create:{
         |      name: "GrandSon2"
         |      friend: {connect:{
         |             name: "Friend"
         |          }}
         |      }}
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    child{
         |       name
         |       friend{
         |          name
         |       }
         |    }
         |  }
         |}}""",
      project
    )

    res2.toString should be(
      """{"data":{"createParent":{"name":"Dad2","child":{"name":"Daughter2","child":{"name":"GrandSon2","friend":{"name":"Friend"}}}}}}""")
  }

  "Dangling Ids" should "be ignored" in {

    val project = SchemaDsl.fromString() {
      """
        |type ZChild{
        |    name: String @unique
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    child: ZChild!
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{ name: "Daughter"}}
         |}){
         |  name,
         |  child{ name}
         |}}""",
      project
    )

    create.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter"}}}}""")

    val delete = server.query(s"""mutation {deleteParent(where: { name: "Dad" }){name}}""", project)

    delete.toString should be("""{"data":{"deleteParent":{"name":"Dad"}}}""")

    val create2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   child: {connect:{name: "Daughter"}}
         |}){
         |  name,
         |  child{ name}
         |}}""",
      project
    )

    create2.toString should be("""{"data":{"createParent":{"name":"Dad2","child":{"name":"Daughter"}}}}""")

  }

  //Fixme https://jira.mongodb.org/browse/SERVER-1068
  "Unique indexes on embedded types" should "work" ignore {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [Child]
        |}
        |
        |type Child @embedded{
        |    name: String @unique
        |}
        |"""
    }

    database.setup(project)

    val create1 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create1.toString should be("""{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"}]}}}""")

    val create2 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad2",
         |   children: {create: [{ name: "Daughter"}, { name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create2.toString should be("""{"data":{"createParent":{"name":"Dad2","children":[{"name":"Daughter"},{"name":"Daughter"}]}}}""")

    val create3 = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create3.toString should be("""{"data":{"createParent":{"name":"Dad2","children":[{"name":"Daughter"},{"name":"Daughter"}]}}}""")

    val update1 = server.query(
      s"""mutation {
         |   updateParent(
         |   where: {name: "Dad"}
         |   data: {
         |      children: {create: [{ name: "Daughter2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    update1.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"}]}}}""")

    val update2 = server.query(
      s"""mutation {
         |   updateParent(
         |   where: {name: "Dad"}
         |   data: {
         |      children: {create: [{ name: "Daughter"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    update2.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"}]}}}""")

  }

  "UpdateMany" should "work between top level types" in {

    val project = SchemaDsl.fromString() {
      """
        |type ZChild{
        |    name: String @unique
        |    test: String
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [ZChild]
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[{ name: "Daughter"},{ name: "Daughter2"}, { name: "Son"},{ name: "Son2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"},{"name":"Son"},{"name":"Son2"}]}}}""")

    val nestedUpdateMany = server.query(
      s"""mutation {
         |   updateParent(
         |   where: { name: "Dad" }
         |   data: {  children: {updateMany:[
         |      {
         |          where:{name_contains:"Daughter"}
         |          data:{test: "UpdateManyDaughters"}
         |      },
         |      {
         |          where:{name_contains:"Son"}
         |          data:{test: "UpdateManySons"}
         |      }
         |   ]
         |  }}
         |){
         |  name,
         |  children{ name, test}
         |}}""",
      project
    )

    nestedUpdateMany.toString should be(
      """{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter","test":"UpdateManyDaughters"},{"name":"Daughter2","test":"UpdateManyDaughters"},{"name":"Son","test":"UpdateManySons"},{"name":"Son2","test":"UpdateManySons"}]}}}""")
  }

  "UpdateMany" should "work with embedded types" in {

    val project = SchemaDsl.fromString() {
      """
        |type ZChild @embedded{
        |    name: String @unique
        |    test: String
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [ZChild]
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[{ name: "Daughter"},{ name: "Daughter2"}, { name: "Son"},{ name: "Son2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"},{"name":"Son"},{"name":"Son2"}]}}}""")

    val nestedUpdateMany = server.query(
      s"""mutation {
         |   updateParent(
         |   where: { name: "Dad" }
         |   data: {  children: {updateMany:[
         |      {
         |          where:{name_contains:"Daughter"}
         |          data:{test: "UpdateManyDaughters"}
         |      },
         |      {
         |          where:{name_contains:"Son"}
         |          data:{test: "UpdateManySons"}
         |      }
         |   ]
         |  }}
         |){
         |  name,
         |  children{ name, test}
         |}}""",
      project
    )

    nestedUpdateMany.toString should be(
      """{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter","test":"UpdateManyDaughters"},{"name":"Daughter2","test":"UpdateManyDaughters"},{"name":"Son","test":"UpdateManySons"},{"name":"Son2","test":"UpdateManySons"}]}}}""")
  }

  "DeleteMany" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type ZChild{
        |    name: String @unique
        |    test: String
        |    parent: Parent
        |}
        |
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [ZChild]
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[{ name: "Daughter"},{ name: "Daughter2"}, { name: "Son"},{ name: "Son2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"},{"name":"Son"},{"name":"Son2"}]}}}""")

    server.query(
      s"""mutation {
         |   updateParent(
         |   where: { name: "Dad" }
         |   data: {  children: {deleteMany:[
         |      {
         |          name_contains:"Daughter"
         |      },
         |      {
         |          name_contains:"Son"
         |      }
         |   ]
         |  }}
         |){
         |  name,
         |  children{ name}
         |}}""",
      project
    )
  }

  "Backrelation bug" should "be fixed" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  nick: String! @unique
        |  memberships: [ListMembership]
        |}
        |
        |type List {
        |  id: ID! @unique
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |  name: String!
        |  memberships: [ListMembership]
        |}
        |
        |type ListMembership {
        |  id: ID! @unique
        |  user: User! @mongoRelation(field: "user")
        |  list: List! @mongoRelation(field: "list")
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation createUser {
  createUser(data: {
    nick: "marcus"
    memberships: {
      create: [
        {
          list: {
            create: {
              name: "Personal Inbox"
            }
          }
        }
      ]
    }
  }){
    nick
  }
}""",
      project
    )

    create.toString should be("""{"data":{"createUser":{"nick":"marcus"}}}""")

    val result = server.query(
      s"""query users {
  users{
    nick
    memberships {
      list {
        name
      }
    }
  }
}""",
      project
    )

    result.toString should be("""{"data":{"users":[{"nick":"marcus","memberships":[{"list":{"name":"Personal Inbox"}}]}]}}""")

  }

  "Lydia deleteMany bug" should "be fixed" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  name: String!
        |  pets: [Dog]
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  author: User @mongoRelation(field: "author")
        |  title: String!
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |}
        |
        |type Walker {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type Dog @embedded {
        |  breed: String!
        |  walker: Walker
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s""" mutation {
         |  createPost(data: {
         |    title:"nice"
         |    author: {
         |      create: {
         |        name: "Lydia"
         |      }
         |    }
         |  }) {
         |    title
         |    author{name}
         |  }
         |} """,
      project
    )

    create.toString should be("""{"data":{"createPost":{"title":"nice","author":{"name":"Lydia"}}}}""")

    val result = server.query(
      s""" mutation {
         |  deleteManyUsers(where: {
         |    pets_some: {
         |      breed: "Test"
         |    }
         |  }) {
         |    count
         |  }
         |} """,
      project
    )

    result.toString should be("""{"data":{"deleteManyUsers":{"count":0}}}""")

    //disallowed for now
//    val result2 = server.query(
//      s""" mutation {
//         |  deleteManyUsers(where: {
//         |    pets_every: {
//         |      breed: "Test"
//         |    }
//         |  }) {
//         |    count
//         |  }
//         |} """,
//      project
//    )
//
//    result2.toString should be("""{"data":{"deleteManyUsers":{"count":1}}}""")

  }

  "Self relations bug" should "be fixed" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  updatedAt: DateTime!
        |  nick: String! @unique
        |}
        |
        |type Todo {
        |  id: ID! @unique
        |  title: String! @unique
        |  comments: [Comment]
        |}
        |
        |type Comment @embedded {
        |  text: String!
        |  user: User! @mongoRelation(field: "user")
        |  snarkyRemark: Comment
        |}"""
    }

    database.setup(project)

    val create = server.query("""mutation{createTodo(data:{title:"todoTitle"}){title}}""", project)
    create.toString should be("""{"data":{"createTodo":{"title":"todoTitle"}}}""")

    val create2 = server.query("""mutation{createUser(data:{nick:"Marcus"}){nick}}""", project)
    create2.toString should be("""{"data":{"createUser":{"nick":"Marcus"}}}""")

    val update = server.query(
      s"""mutation c{
  updateTodo(
    where: { title: "todoTitle" }
    data: {
      comments: {
        create: [
          {
            text:"This is very important"
            user: {
              connect: {
                nick: "Marcus"
              }
            }
            snarkyRemark: {
              create: {
                text:"This is very very imporanto!"
                user: {
                  connect: {nick:"Marcus"}
                }
              }
            }
          }
        ]
      }
    }
  ){
    title
  }
}""",
      project
    )

    update.toString should be("""{"data":{"updateTodo":{"title":"todoTitle"}}}""")

    val result = server.query(
      s"""query commentsOfAUser {
         |  todoes(where: {
         |    comments_some: {
         |      text_contains: "This"
         |    }
         |  }) {
         |    title
         |    comments {
         |      text
         |      snarkyRemark{
         |         text
         |         user{
         |            nick
         |         }
         |      }
         |    }
         |  }
         |} """,
      project
    )

    result.toString should be(
      """{"data":{"todoes":[{"title":"todoTitle","comments":[{"text":"This is very important","snarkyRemark":{"text":"This is very very imporanto!","user":{"nick":"Marcus"}}}]}]}}""")

  }

  "Connecting several times" should "not error and only connect the item once" in {

    val project = SchemaDsl.fromString() {
      """
        |type Post {
        |  id: ID! @unique
        |  authors: [AUser]
        |  title: String! @unique
        |}
        |
        |type AUser {
        |  id: ID! @unique
        |  name: String! @unique
        |  posts: [Post] @mongoRelation(field: "posts")
        |}"""
    }

    database.setup(project)

    val createPost = server.query(s""" mutation {createPost(data: {title:"Title"}) {title}} """, project)
    val createUser = server.query(s""" mutation {createAUser(data: {name:"Author"}) {name}} """, project)

    val result1 = server.query(s""" mutation {updateAUser(where: { name: "Author"}, data:{posts:{connect:{title: "Title"}}}) {name}} """, project)
    val result2 = server.query(s""" mutation {updateAUser(where: { name: "Author"}, data:{posts:{connect:{title: "Title"}}}) {name}} """, project)
    val result3 = server.query(s""" mutation {updateAUser(where: { name: "Author"}, data:{posts:{connect:{title: "Title"}}}) {name}} """, project)

    server.query("""query{aUsers{name, posts{title}}}""", project).toString should be("""{"data":{"aUsers":[{"name":"Author","posts":[{"title":"Title"}]}]}}""")
  }

  "Join Relation Filter on one to one relation" should "work on one level" in {

    val project = SchemaDsl.fromString() {
      """
        |type Post {
        |  id: ID! @unique
        |  author: AUser
        |  title: String! @unique
        |}
        |
        |type AUser {
        |  id: ID! @unique
        |  name: String! @unique
        |  int: Int
        |  post: Post @mongoRelation(field: "posts")
        |}"""
    }

    database.setup(project)

    val createPost  = server.query(s""" mutation {createPost(data: {title:"Title1"}) {title}} """, project)
    val createPost2 = server.query(s""" mutation {createPost(data: {title:"Title2"}) {title}} """, project)
    val createUser  = server.query(s""" mutation {createAUser(data: {name:"Author1", int: 5}) {name}} """, project)
    val createUser2 = server.query(s""" mutation {createAUser(data: {name:"Author2", int: 4}) {name}} """, project)

    val result1 = server.query(s""" mutation {updateAUser(where: { name: "Author1"}, data:{post:{connect:{title: "Title1"}}}) {name}} """, project)
    val result2 = server.query(s""" mutation {updateAUser(where: { name: "Author2"}, data:{post:{connect:{title: "Title2"}}}) {name}} """, project)

    server.query("""query{aUsers{name, post{title}}}""", project).toString should be(
      """{"data":{"aUsers":[{"name":"Author1","post":{"title":"Title1"}},{"name":"Author2","post":{"title":"Title2"}}]}}""")

    server.query("""query{posts {title, author {name}}}""", project).toString should be(
      """{"data":{"posts":[{"title":"Title1","author":{"name":"Author1"}},{"title":"Title2","author":{"name":"Author2"}}]}}""")

    val res = server.query("""query{aUsers(where:{ post:{title_ends_with: "1"}, name_starts_with: "Author", int: 5}){name, post{title}}}""", project)
    res.toString should be("""{"data":{"aUsers":[{"name":"Author1","post":{"title":"Title1"}}]}}""")
  }

  "Join Relation Filter on many to many relation" should "work on one level" in {

    val project = SchemaDsl.fromString() {
      """
        |type Post {
        |  id: ID! @unique
        |  authors: [AUser]
        |  title: String! @unique
        |}
        |
        |type AUser {
        |  id: ID! @unique
        |  name: String! @unique
        |  posts: [Post] @mongoRelation(field: "posts")
        |}"""
    }

    database.setup(project)

    val createPost  = server.query(s""" mutation {createPost(data: {title:"Title1"}) {title}} """, project)
    val createPost2 = server.query(s""" mutation {createPost(data: {title:"Title2"}) {title}} """, project)
    val createUser  = server.query(s""" mutation {createAUser(data: {name:"Author1"}) {name}} """, project)
    val createUser2 = server.query(s""" mutation {createAUser(data: {name:"Author2"}) {name}} """, project)

    val result1 =
      server.query(s""" mutation {updateAUser(where: { name: "Author1"}, data:{posts:{connect:[{title: "Title1"},{title: "Title2"}]}}) {name}} """, project)
    val result2 =
      server.query(s""" mutation {updateAUser(where: { name: "Author2"}, data:{posts:{connect:[{title: "Title1"},{title: "Title2"}]}}) {name}} """, project)

    server.query("""query{aUsers{name, posts{title}}}""", project).toString should be(
      """{"data":{"aUsers":[{"name":"Author1","posts":[{"title":"Title1"},{"title":"Title2"}]},{"name":"Author2","posts":[{"title":"Title1"},{"title":"Title2"}]}]}}""")

    server.query("""query{posts {title, authors {name}}}""", project).toString should be(
      """{"data":{"posts":[{"title":"Title1","authors":[{"name":"Author1"},{"name":"Author2"}]},{"title":"Title2","authors":[{"name":"Author1"},{"name":"Author2"}]}]}}""")

    val res = server.query("""query{aUsers(where:{name_starts_with: "Author2", posts_some:{title_ends_with: "1"}}){name, posts{title}}}""", project)
    res.toString should be("""{"data":{"aUsers":[{"name":"Author2","posts":[{"title":"Title1"},{"title":"Title2"}]}]}}""")
  }

  "Deeply nested create" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  name: String!
        |  pets: [Dog]
        |  posts: [Post]
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  author: User @mongoRelation(field: "author")
        |  title: String!
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |}
        |
        |type Walker {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type Dog @embedded {
        |  breed: String!
        |  walker: Walker @mongoRelation(field: "dogtowalker")
        |}"""
    }

    database.setup(project)

    val query = """mutation create {
                  |  createUser(
                  |    data: {
                  |      name: "User"
                  |      posts: { create: [{ title: "Title 1" }, { title: "Title 2" }] }
                  |      pets: {
                  |        create: [
                  |          { breed: "Breed 1", walker: { create: { name: "Walker 1" } } }
                  |          { breed: "Breed 1", walker: { create: { name: "Walker 1" } } }
                  |        ]
                  |      }
                  |    }
                  |  ) {
                  |    name
                  |    posts {
                  |      title
                  |    }
                  |    pets {
                  |      breed
                  |      walker {
                  |        name
                  |      }
                  |    }
                  |  }
                  |}"""

    server.query(query, project).toString should be(
      """{"data":{"createUser":{"name":"User","posts":[{"title":"Title 1"},{"title":"Title 2"}],"pets":[{"breed":"Breed 1","walker":{"name":"Walker 1"}},{"breed":"Breed 1","walker":{"name":"Walker 1"}}]}}}""")
  }

  "Fancy filter" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  name: String!
        |  pets: [Dog]
        |  posts: [Post]
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  author: User @mongoRelation(field: "author")
        |  title: String!
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |}
        |
        |type Walker {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type Dog @embedded {
        |  breed: String!
        |  walker: Walker @mongoRelation(field: "dogtowalker")
        |}"""
    }

    database.setup(project)

    val query = """mutation create {
                  |  createUser(
                  |    data: {
                  |      name: "User"
                  |      posts: { create: [{ title: "Title 1" }, { title: "Title 2" }] }
                  |      pets: {
                  |        create: [
                  |          { breed: "Breed 1", walker: { create: { name: "Walker 1" } } }
                  |          { breed: "Breed 1", walker: { create: { name: "Walker 1" } } }
                  |        ]
                  |      }
                  |    }
                  |  ) {
                  |    name
                  |    posts {
                  |      title
                  |    }
                  |    pets {
                  |      breed
                  |      walker {
                  |        name
                  |      }
                  |    }
                  |  }
                  |}"""

    server.query(query, project).toString should be(
      """{"data":{"createUser":{"name":"User","posts":[{"title":"Title 1"},{"title":"Title 2"}],"pets":[{"breed":"Breed 1","walker":{"name":"Walker 1"}},{"breed":"Breed 1","walker":{"name":"Walker 1"}}]}}}""")

    val query2 = """query withFilter {
                   |  users(
                   |    where: {
                   |      name: "User"
                   |      posts_some: { title_ends_with: "1" }
                   |      pets_some: { breed: "Breed 1", walker: { name: "Walker 2" } }
                   |    }
                   |  ) {
                   |    name
                   |    posts {
                   |      title
                   |    }
                   |    pets {
                   |      breed
                   |      walker {
                   |        name
                   |      }
                   |    }
                   |  }
                   |}"""

    server.query(query2, project).toString should be("""{"data":{"users":[]}}""")

    val query3 = """query withFilter {
                   |  users(
                   |    where: {
                   |      name: "User"
                   |      posts_some: { title_ends_with: "1" }
                   |      pets_some: { breed: "Breed 1", walker: { name: "Walker 1" } }
                   |    }
                   |  ) {
                   |    name
                   |    posts {
                   |      title
                   |    }
                   |    pets {
                   |      breed
                   |      walker {
                   |        name
                   |      }
                   |    }
                   |  }
                   |}"""

    server.query(query3, project).toString should be("""{"data":{"users":[]}}""")

  }

}
