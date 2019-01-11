package com.prisma.api.mutations

import java.util.UUID

import com.prisma.{IgnoreMongo, IgnoreMySql}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpsertPostgresUUIDSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  "Upserting an item with an id field of type UUID" should "work" taggedAs (IgnoreMySql, IgnoreMongo) in {
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
