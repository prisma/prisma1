package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class InsertingNullInRequiredFieldsSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Setting a required value to null" should "throw a proper error" in {
    val project = SchemaDsl.fromString() {
      """type A {
        |  id: ID! @unique
        |  b: String! @unique
        |  key: String! @unique
        |}
      """
    }
    database.setup(project)

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: "abc"
        |    key: "abc"
        |  }) {
        |    id
        |  }
        |}""",
      project
    )

    server.queryThatMustFail(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      key: null
        |    }) {
        |    id
        |  }
        |}""",
      project,
      3020
    )
  }

  "Setting an optional value to null" should "work" in {
    val project = SchemaDsl.fromString() {
      """type A {
        |  id: ID! @unique
        |  b: String! @unique
        |  key: String @unique
        |}
      """
    }
    database.setup(project)

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: "abc"
        |    key: "abc"
        |  }) {
        |    id
        |  }
        |}""",
      project
    )

    server.query(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      key: null
        |    }) {
        |    id
        |  }
        |}""",
      project
    )

    server.query("""query{as{b,key}}""", project).toString should be("""{"data":{"as":[{"b":"abc","key":null}]}}""")
  }

}
