package writes

import org.scalatest.{FlatSpec, Matchers}
import util._

class WhereAndUpdateSpec extends FlatSpec with Matchers with ApiSpecBase {

  "Updating the unique value used to find an item" should "work" in {

    val project = ProjectDsl.fromString {
      """model Test {
        |   id      String @id @default(cuid())
        |   unique  Int    @unique
        |   name    String
        |}"""
    }

    database.setup(project)

    server.query(s"""mutation {createTest(data: {unique: 1, name: "Test"}){ unique}}""", project)

    server.query(s"""query {test(where:{unique:1}){ unique}}""", project).toString should be("""{"data":{"test":{"unique":1}}}""")

    val res = server.query(s"""mutation {updateTest( where: { unique: 1 } data: {unique: 2}){unique}}""", project).toString
    res should be("""{"data":{"updateTest":{"unique":2}}}""")
  }
}
