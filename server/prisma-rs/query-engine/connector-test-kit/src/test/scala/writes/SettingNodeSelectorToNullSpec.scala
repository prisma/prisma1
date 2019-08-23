package writes

import org.scalatest.{FlatSpec, Matchers}
import util._

class SettingNodeSelectorToNullSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Setting a where value to null " should " work when there is no further nesting " in {
    val project = ProjectDsl.fromString {
      """
        |model A {
        |  id  String  @id @default(cuid())
        |  b   String? @unique
        |  key String  @unique
        |}
      """
    }
    database.setup(project)

    server.query(
      """mutation a {
        |  createA(data: {
        |    b: "abc"
        |    key: "abc"
        |  }) {
        |    id
        |  }
        |}""",
      project
    )

    val res = server.query(
      """mutation b {
        |  updateA(
        |    where: { b: "abc" }
        |    data: {
        |      b: null
        |    }) {
        |    b
        |  }
        |}""",
      project
    )

    res.toString() should be("""{"data":{"updateA":{"b":null}}}""")
  }
}
