package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class SettingNodeSelectorToNullSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Setting a where value to null " should " work when there is no further nesting " in {
    val project = SchemaDsl.fromString() {
      """
        |type A {
        |  id: ID! @unique
        |  b: String @unique
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

    val res = server.query(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      b: null
        |    }) {
        |    b
        |  }
        |}""",
      project
    )

    res.toString() should be("""{"data":{"updateA":{"b":null}}}""")
  }

  "Setting a where value to null " should "should only update one if there are several nulls for the specified node selector" in {
    val project = SchemaDsl.fromString() {
      """
        |type A {
        |  id: ID! @unique
        |  b: String @unique
        |  key: String! @unique
        |  c: C
        |}
        |
        |type C {
        |  id: ID! @unique
        |  c: String
        |}
      """
    }
    database.setup(project)

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: "abc"
        |    key: "abc"
        |    c: {
        |       create:{ c: "C"}
        |    }
        |  }) {
        |    id
        |    key,
        |    b,
        |    c {c}
        |  }
        |}""",
      project
    )

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: null
        |    key: "abc2"
        |    c: {
        |       create:{ c: "C2"}
        |    }
        |  }) {
        |    key,
        |    b,
        |    c {c}
        |  }
        |}""",
      project
    )

    server.query(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      b: null
        |      c: {update:{c:"NewC"}}
        |    }) {
        |    b
        |    c{c}
        |  }
        |}""",
      project
    )

    val result = server.query(
      """
        |{
        | as {
        |   b
        |   c {
        |     c
        |   }
        | }
        |}
      """.stripMargin,
      project
    )

    result.toString should be("""{"data":{"as":[{"b":null,"c":{"c":"NewC"}},{"b":null,"c":{"c":"C2"}}]}}""")
  }

}
