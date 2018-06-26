package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteScalarListsSpec extends FlatSpec with Matchers with ApiSpecBase {

  "The delete  mutation" should "delete the item matching the where clause" in {

    val project: Project = SchemaDsl.fromString() {
      """type TestModel {
        | name: String! @unique
        | list: [Int!]!
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
  }

}
