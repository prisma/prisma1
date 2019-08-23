package queries.filters.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class FilterUnwrapSomeSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project: Project = ProjectDsl.fromString { """model Item {
                                                    |  id       String    @id @default(cuid())
                                                    |  name     String?   @unique
                                                    |  subItems SubItem[]
                                                    |}
                                                    |
                                                    |model SubItem {
                                                    |  id   String  @id @default(cuid())
                                                    |  name String? @unique
                                                    |  item Item?   @relation(references: [id])
                                                    |}""".stripMargin }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    populate
  }

  "Many filter" should "work" in {

    val query = """mutation {
                  |  updateItem(
                  |    data: {
                  |      subItems: {
                  |        deleteMany: {
                  |          name_in: ["TEST1", "TEST2"]
                  |        }
                  |      }
                  |    }
                  |    where: { name: "Top" }
                  |  ) {
                  |    name
                  |    subItems {
                  |      name
                  |    }
                  |  }
                  |}"""

    server.query(query, project)
  }

  def populate: Unit = {
    server.query(
      s"""mutation {
         |  createItem(
         |    data: { name: "Top", subItems: { create: [{ name: "TEST1" }, { name: "TEST2" }] } }
         |  ) {
         |    name
         |    subItems {
         |      name
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
  }
}
