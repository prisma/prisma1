package writes.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NonEmbeddedSettingNodeSelectorToNullSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "Setting a where value to null " should "should only update one if there are several nulls for the specified node selector" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |model A {
        |  id  String  @id @default(cuid())
        |  b   String? @unique
        |  key String  @unique
        |  c   C?      @relation(references: [id])
        |}
        |
        |model C {
        |  id String  @id @default(cuid())
        |  c  String?
        |}
      """
    }
    database.setup(project)

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: "abc"
        |    key: "abc"
        |    c: {
        |       create:{ c: "C"}
        |    }
        |  }) {
        |    id
        |    key,
        |    b,
        |    c {c}
        |  }
        |}""",
      project
    )

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: null
        |    key: "abc2"
        |    c: {
        |       create:{ c: "C2"}
        |    }
        |  }) {
        |    key,
        |    b,
        |    c {c}
        |  }
        |}""",
      project
    )

    server.query(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      b: null
        |      c: {update:{c:"NewC"}}
        |    }) {
        |    b
        |    c{c}
        |  }
        |}""",
      project
    )

    val result = server.query(
      """
        |{
        | as {
        |   b
        |   c {
        |     c
        |   }
        | }
        |}
      """.stripMargin,
      project
    )

    result.toString should be("""{"data":{"as":[{"b":null,"c":{"c":"NewC"}},{"b":null,"c":{"c":"C2"}}]}}""")
  }

}
