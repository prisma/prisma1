package com.prisma.api.mutations

import java.util.UUID

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, UuidIdCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpsertPostgresUUIDSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability, UuidIdCapability)

  "Upserting an item with an id field of type UUID" should "work" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
         |type Todo {
         |  id: UUID! @id
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
