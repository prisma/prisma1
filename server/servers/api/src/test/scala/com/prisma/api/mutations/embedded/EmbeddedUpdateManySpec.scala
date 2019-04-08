package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedUpdateManySpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(EmbeddedTypesCapability)

  "UpdateMany" should "work with embedded types" in {

    val project = SchemaDsl.fromStringV11() {
      """
        |type ZChild @embedded {
        |    id: ID! @id
        |    name: String
        |    test: String
        |}
        |
        |type Parent {
        |    id: ID! @id
        |    name: String @unique
        |    children: [ZChild]
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[{ name: "Daughter"},{ name: "Daughter2"}, { name: "Son"},{ name: "Son2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"},{"name":"Son"},{"name":"Son2"}]}}}""")

    val nestedUpdateMany = server.query(
      s"""mutation {
         |   updateParent(
         |   where: { name: "Dad" }
         |   data: {  children: {updateMany:[
         |      {
         |          where:{name_contains:"Daughter"}
         |          data:{test: "UpdateManyDaughters"}
         |      },
         |      {
         |          where:{name_contains:"Son"}
         |          data:{test: "UpdateManySons"}
         |      }
         |   ]
         |  }}
         |){
         |  name,
         |  children{ name, test}
         |}}""",
      project
    )

    nestedUpdateMany.toString should be(
      """{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter","test":"UpdateManyDaughters"},{"name":"Daughter2","test":"UpdateManyDaughters"},{"name":"Son","test":"UpdateManySons"},{"name":"Son2","test":"UpdateManySons"}]}}}""")
  }
}
