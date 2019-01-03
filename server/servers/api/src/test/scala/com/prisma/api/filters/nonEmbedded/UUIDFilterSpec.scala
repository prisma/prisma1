package com.prisma.api.filters.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.UuidIdCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UUIDFilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(UuidIdCapability)

  "Using a UUID field in where clause" should "work" in {
    val project: Project = SchemaDsl.fromString() { """
                                                      |type User {
                                                      |  id: UUID! @unique
                                                      |  name: String!
                                                      |}""".stripMargin }
    database.setup(project)
    server.query("""query {users(where: { id: "a3f7bcd1-3ae7-4706-913a-9cfe5ed7e7b6" }) {id}}""", project).toString should be("""{"data":{"users":[]}}""")
  }
}
