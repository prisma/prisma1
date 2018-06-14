package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class JooqTestingSpec extends FlatSpec with Matchers with ApiSpecBase {

  "the multi item query" should "work 1" in {

    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |  b: B
        |}
        |
        |type B {
        |  id: ID! @unique
        |  int: Int
        |  rel: User
        |}"""
    }

    database.setup(project)

    server.query(
      s"""{
         |  users (where: {b: null}){
         |    name
         |  }
         |}""",
      project
    )

  }

  "the multi item query" should "work 2" in {

    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |  b: B
        |}
        |
        |type B {
        |  id: ID! @unique
        |  int: Int
        |  rel: User
        |}"""
    }

    database.setup(project)

    server.query("""mutation{createB(data:{int: 12}){id}}""", project)

    server.query(
      s"""{
         |  bs (where: {int: 12}){
         |    id
         |  }
         |}""",
      project
    )
  }

  "Selecting nested" should "work 2" in {

    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |  b: [B!]!
        |}
        |
        |type B {
        |  id: ID! @unique
        |  int: Int
        |  rel: User
        |}"""
    }

    database.setup(project)

    server.query("""mutation{createUser(data:{name: "test", b:{create:{int: 12}}}){id}}""", project)

    server.query(
      s"""{
         |  users{
         |    name
         |    b (first:10){
         |      int
         |    }
         |  }
         |}""",
      project
    )
  }
}
