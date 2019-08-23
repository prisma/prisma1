package com.prisma.api.filters.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, Prisma2Capability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class RelationIsNullSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities  = Set(JoinRelationLinksCapability)
  override def doNotRunForCapabilities = Set(Prisma2Capability)

  val project = SchemaDsl.fromStringV11() {
    """
      |type Message {
      |  id: ID! @id
      |  image: Image @relation(link: INLINE, name: "MessageImageRelation")
      |  messageName: String
      |}
      |
      |type Image {
      |  id: ID! @id
      |  message: Message @relation(name: "MessageImageRelation")
      |  imageName: String
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach() = {
    super.beforeEach()
    database.truncateProjectTables(project)

    // add data
    server.query(
      """mutation {createMessage(
        |     data: {
        |       messageName: "message 1",
        |       }
        |){messageName}}""",
      project = project
    )

    server.query(
      """mutation {createMessage(
        |     data: {
        |       messageName: "message 2",
        |       image:{create:{imageName:"image 1"}}
        |       }
        |){messageName}}""",
      project = project
    )

    server.query(
      """mutation {createImage(
        |     data: {
        |       imageName: "image 2"
        |       }
        |){imageName}}""",
      project = project
    )

  }

  "Filtering on whether a relation is null" should "work" in {
    server
      .query(
        query = """query {
                           |  images(where: { message: null }) {
                           |    imageName
                           |  }
                           |}""",
        project = project
      )
      .toString should be("""{"data":{"images":[{"imageName":"image 2"}]}}""")
  }

  "Filtering on whether a relation is null" should "work 2" in {
    server
      .query(
        query = """query {
                  |  messages(where: { image: null }) {
                  |    messageName
                  |  }
                  |}""",
        project = project
      )
      .toString should be("""{"data":{"messages":[{"messageName":"message 1"}]}}""")
  }
}
