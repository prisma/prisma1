package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.ScalarListsCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteScalarListsSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability)

  "A toplevel delete  mutation" should "also delete ListTable entries" in {

    val project: Project = SchemaDsl.fromString() {
      """type TestModel {
        | id: ID! @unique
        | name: String! @unique
        | list: [Int]
        |}"""
    }

    database.setup(project)

    server.query(
      """mutation {
        |  createTestModel(
        |    data: { name: "test", list: {set: [1,2,3]} }
        |  ){
        |    name
        |    list
        |  }
        |}
      """,
      project
    )

    server.query("""mutation{deleteTestModel(where:{name:"test" }){name}}""", project)

    server.query("""query{testModels{name}}""", project).toString() should be("""{"data":{"testModels":[]}}""")
  }

  "A delete Many  mutation" should "also delete ListTable entries" in {

    val project: Project = SchemaDsl.fromString() {
      """type Top {
        | id: ID! @unique
        | name: String! @unique
        | list: [Int]
        |}"""
    }

    database.setup(project)

    server.query("""mutation {createTop(data: { name: "test", list: {set: [1,2,3]}}){name, list}}""", project)
    server.query("""mutation {createTop(data: { name: "test2", list: {set: [1,2,3]}}){name, list}}""", project)
    server.query("""mutation {createTop(data: { name: "test3", list: {set: [1,2,3]}}){name, list}}""", project)

    val res = server.query("""mutation{deleteManyTops(where:{name_contains:"2" }){count}}""", project)

    res.toString should be("""{"data":{"deleteManyTops":{"count":1}}}""")
  }
}
