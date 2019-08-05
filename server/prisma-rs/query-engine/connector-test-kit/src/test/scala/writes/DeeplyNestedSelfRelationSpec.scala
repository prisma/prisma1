package writes

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class DeeplyNestedSelfRelationSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "A deeply nested self relation create" should "be executed completely" in {
    val project = ProjectDsl.fromString {
      """model User {
      |  id       String @id @default(cuid())
      |  name     String @unique
      |  parent   User?  @relation(name: "Users", references: [id])
      |  children User[] @relation(name: "Users")
      |}"""
    }

    database.setup(project)

    val create = server.query(
      """mutation {
                   |  createUser(
                   |    data: {
                   |      name: "A"
                   |      children: {
                   |        create: [
                   |          { name: "B",
                   |            children: {
                   |              create: [{ name: "C" }]
                   |            }
                   |        }]
                   |      }
                   |    }
                   |  ) {
                   |    name
                   |    parent {name}
                   |    children {
                   |      name
                   |      parent {name}
                   |      children {
                   |        name
                   |        parent {name}
                   |        children {
                   |          parent {name}
                   |          id
                   |        }
                   |      }
                   |    }
                   |  }
                   |}""",
      project
    )

    create.toString should be(
      """{"data":{"createUser":{"name":"A","parent":null,"children":[{"name":"B","parent":{"name":"A"},"children":[{"name":"C","parent":{"name":"B"},"children":[]}]}]}}}""")

    val query = server.query("""{
                   |  users{
                   |    name
                   |  }
                   |}
                   |""",
                             project)

    query.toString should be("""{"data":{"users":[{"name":"A"},{"name":"B"},{"name":"C"}]}}""")

  }
}
