package writes

import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.{JoinRelationLinksCapability, UuidIdCapability}
import util._

class UpsertPostgresUUIDSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability, UuidIdCapability)

  "Upserting an item with an id field of model UUID" should "work" in {
    val project = ProjectDsl.fromString {
      s"""
         |model Todo {
         |  id     String @id @default(uuid())
         |  title  String
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
