package com.prisma.api.mutations

import com.prisma.IgnorePassive
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoPrototypingSpec extends FlatSpec with Matchers with ApiSpecBase {

  "To one relations" should "work" taggedAs (IgnorePassive) in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |   createdAt: DateTime!
        |}
        |
        |type Middle {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   bottom: Bottom
        |   createdAt: DateTime!
        |}
        |
        |type Bottom {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   updatedAt: DateTime!
        |}"""
    }

    database.setup(project)

    server.query(
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
         |  createdAt
         |  middle{
         |    unique,
         |    createdAt,
         |    bottom{
         |      unique
         |      updatedAt
         |    }
         |  }
         |}}""".stripMargin,
      project
    )
  }

  "To many relations" should "work" taggedAs (IgnorePassive) in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle!]!
        |}
        |
        |type Middle {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   bottom: [Bottom!]!
        |}
        |
        |type Bottom {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(
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
  }

  "ListValues" should "work" taggedAs (IgnorePassive) in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   ints: [Int!]!
        |}"""
    }

    database.setup(project)

    server.query(
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
  }
}
