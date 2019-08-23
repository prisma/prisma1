package queries.filters.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util._
import util.ConnectorCapability.UuidIdCapability

class UUIDFilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(UuidIdCapability)

  "Using a UUID field in where clause" should "work" in {
    val project: Project = ProjectDsl.fromString { """
                                                      |model User {
                                                      |  id   String @id @default(uuid())
                                                      |  name String
                                                      |}""".stripMargin }
    database.setup(project)
    server.query("""query {users(where: { id: "a3f7bcd1-3ae7-4706-913a-9cfe5ed7e7b6" }) {id}}""", project).toString should be("""{"data":{"users":[]}}""")
  }
}
