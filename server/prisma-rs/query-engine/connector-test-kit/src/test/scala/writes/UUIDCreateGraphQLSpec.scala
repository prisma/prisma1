package writes

import java.util.UUID

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.UuidIdCapability
import util._

class UUIDCreateGraphQLSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(UuidIdCapability)

  "Creating an item with an id field of model UUID" should "work" in {
    val project = ProjectDsl.fromString {
      s"""
         |model Todo {
         |  id    String @id @default(uuid())
         |  title String
         |}
       """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  createTodo(data: { title: "the title" }){
        |    id
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.createTodo.title") should equal("the title")
    val theUUID = result.pathAsString("data.createTodo.id")
    UUID.fromString(theUUID) // should just not blow up
  }

  "Fetching a UUID field that is null" should "work" in {
    val project = ProjectDsl.fromString {
      s"""
         |model TableA {
         |    id    String  @id @default(uuid())
         |    name  String
         |    b     String? @unique
         |}""".stripMargin
    }
    database.setup(project)

    server.query("""mutation {createTableA(data: {name:"testA"}){id}}""", project)

    server.query("""query {tableAs {name, b}}""", project).toString should be("""{"data":{"tableAs":[{"name":"testA","b":null}]}}""")
  }
}
