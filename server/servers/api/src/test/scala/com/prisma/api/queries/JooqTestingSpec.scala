package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class JooqTestingSpec extends FlatSpec with Matchers with ApiSpecBase {

  "the multi item query" should "return empty list" in {

    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |  b: B
        |}
        |
        |type B {
        |  id: ID! @unique
        |  rel: User
        |  c: C
        |}
        |
        |type C {
        |  id: ID! @unique
        |  b: B
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

    server.query(
      s"""{
         |  users (where: {name: "Paul"}){
         |    name
         |  }
         |}""",
      project
    )

  }
}
