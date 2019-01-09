package com.prisma.api.queries

import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.MongoConnectorTag
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoObjectIdSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForConnectors: Set[ConnectorTag] = Set(MongoConnectorTag)

  "Using an invalid MongoObjectId" should "return a proper error" in {

    val project = SchemaDsl.fromString() {
      """type Test{
        |   id: ID! @unique
        |   int: Int
        |}"""
    }

    database.setup(project)

    val result = server.query(s"""mutation {createTest(data:{int: 5}){int, id}}""", project)

    server.queryThatMustFail(
      """query{test(where: {id: "DOES NOT EXIST"}){id}}""",
      project,
      errorCode = 3044,
      errorContains = """You provided an ID that was not a valid MongoObjectId: DOES NOT EXIST""""
    )
  }

}
