package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoPrototypingSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Testing stuff" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   bottom: Bottom
        |}
        |
        |
        |type Bottom {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(s"""mutation {createTop(data: {unique: 1, name: "Top", bottom: {create:{unique: 11, name: "Bottom"}}}){ bottom{unique}}}""", project)
  }

  "Testing stuff" should "work2" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   bottoms: [Bottom!]!
        |}
        |
        |
        |type Bottom {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {createTop(data: {unique: 1, name: "Top", bottoms: {create:[{unique: 11, name: "Bottom"},{unique: 12, name: "Bottom2"}]}}){ bottoms{unique}}}""",
      project
    )
  }
}
