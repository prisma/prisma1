package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, ScalarListsCapability}
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedDeleteScalarListsSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(ScalarListsCapability, EmbeddedTypesCapability)

  "A nested delete  mutation" should "also delete ListTable entries" in {

    val project: Project = SchemaDsl.fromStringV11() {
      """type Top {
        | id: ID! @id
        | name: String! @unique
        | topList: [Int]
        | bottom: Bottom
        |}
        |
        |type Bottom @embedded {
        | id: ID! @id
        | name: String!
        | bottomList: [Int]
        |}"""
    }

    database.setup(project)

    val create = server.query(
      """mutation {
        |  createTop(
        |    data: { name: "test", topList: {set: [1,2,3]} bottom: {create: {name: "test2", bottomList: {set: [1,2,3]}} }}
        |  ){
        |    name
        |    topList
        |    bottom{name, bottomList}
        |  }
        |}
      """,
      project
    )

    create.toString should be("""{"data":{"createTop":{"name":"test","topList":[1,2,3],"bottom":{"name":"test2","bottomList":[1,2,3]}}}}""")

    val deleteBottom = server.query("""mutation{updateTop(where:{name:"test" }data: {bottom: {delete: true}}){name, bottom{name}}}""", project)

    deleteBottom.toString should be("""{"data":{"updateTop":{"name":"test","bottom":null}}}""")

    val deleteTop = server.query("""mutation{deleteTop(where:{name:"test" }){name}}""", project)

    deleteTop.toString should be("""{"data":{"deleteTop":{"name":"test"}}}""")
  }

}
