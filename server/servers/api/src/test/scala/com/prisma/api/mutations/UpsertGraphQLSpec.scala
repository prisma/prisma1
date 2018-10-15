package com.prisma.api.mutations

import java.util.UUID

import com.prisma.IgnoreMySql
import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.ApiConnectorCapability
import com.prisma.api.connector.ApiConnectorCapability.JoinRelationsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpsertGraphQLSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ApiConnectorCapability] = Set(JoinRelationsCapability)

  "Upserting an item with an id field of type UUID" should "work" taggedAs (IgnoreMySql) in {
    val project = SchemaDsl.fromString() {
      s"""
         |type Todo {
         |  id: UUID! @unique
         |  title: String!
         |}
       """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  upsertTodo(
        |    where: {id: "00000000-0000-0000-0000-000000000000"}
        |    create: { title: "the title" }
        |    update: { title: "the updated title" }
        |  ){
        |    id
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertTodo.title") should equal("the title")
    val theUUID = result.pathAsString("data.upsertTodo.id")
    UUID.fromString(theUUID) // should just not blow up
  }
}
