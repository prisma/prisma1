package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class WhereAndUpdateSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Updating the unique value used to find an item" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Test {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    server.query(s"""mutation {createTest(data: {unique: 1, name: "Test"}){ unique}}""", project)

    server.query(s"""query {test(where:{unique:1}){ unique}}""", project).toString should be("""{"data":{"test":{"unique":1}}}""")

    val res = server.query(s"""mutation {updateTest( where: { unique: 1 } data: {unique: 2}){unique}}""", project).toString
    res should be("""{"data":{"updateTest":{"unique":2}}}""")
  }
}
