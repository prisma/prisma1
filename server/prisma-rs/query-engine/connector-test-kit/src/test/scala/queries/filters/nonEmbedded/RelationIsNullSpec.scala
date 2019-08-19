package queries.filters.nonEmbedded

import util._
import org.scalatest._
import util.ConnectorCapability.JoinRelationLinksCapability

class RelationIsNullSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromStringV11() {
    """
      |model Message {
      |  id          String  @id @default(cuid())
      |  image       Image?  @relation(name: "MessageImageRelation", references: [id])
      |  messageName String?
      |}
      |
      |model Image {
      |  id        String   @id @default(cuid())
      |  message   Message? @relation(name: "MessageImageRelation")
      |  imageName String?
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
