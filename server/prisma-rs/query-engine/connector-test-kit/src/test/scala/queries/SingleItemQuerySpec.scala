package queries

import org.scalatest.{Matchers, WordSpecLike}
import util._

class SingleItemQuerySpec extends WordSpecLike with Matchers with ApiSpecBase {

//  "should return null if the id does not exist" in {
//    ifIsNotPrisma2 {
//      val project = SchemaDsl.fromStringV11() {
//        """model Todo {
//        |  id String @id @default(cuid())
//        |  title: String!
//        |}
//      """.stripMargin
//      }
//      database.setup(project)
//
//      val result = server.query(
//        s"""{
//           |  todo(where: {id: "5beea4aa6183dd734b2dbd9b"}){
//           |    ...todoFields
//           |  }
//           |}
//           |
//           |fragment todoFields on Todo {
//           |  id
//           |  title
//           |}
//           |""".stripMargin,
//        project
//      )
//
//      result.toString should equal("""{"data":{"todo":null}}""")
//    }
//  }

  "should work by id" in {
    val project = SchemaDsl.fromStringV11() {
      """model Todo {
        |  id    String @id @default(cuid())
        |  title String
        |}
      """.stripMargin
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(s"""mutation {
        |  createTodo(data: {title: "$title"}) {
        |    id
        |  }
        |}""".stripMargin,
             project)
      .pathAsString("data.createTodo.id")

    val result = server.query(s"""{
        |  todo(where: {id: "$id"}){
        |    id
        |    title
        |  }
        |}""".stripMargin,
                              project)

    result.pathAsString("data.todo.title") should equal(title)
  }

  "should work by any unique field" in {
    val project = SchemaDsl.fromStringV11() {
      """model Todo {
        |  id    String @id @default(cuid())
        |  title String
        |  alias String @unique
        |}
      """.stripMargin
    }
    database.setup(project)

    val title = "Hello World!"
    val alias = "my-alias"
    server.query(
      s"""mutation {
         |  createTodo(data: {title: "$title", alias: "$alias"}) {
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    val result = server.query(
      s"""{
          |  todo(where: {alias: "$alias"}){
          |    id
          |    title
          |  }
          |}""".stripMargin,
      project
    )

    result.pathAsString("data.todo.title") should equal(title)
  }

  "should respect custom db names" in {
//    ifIsNotPrisma2 {
//      val project = SchemaDsl.fromStringV11() {
//        """
//          |model Todo @db(name: "my_table") {
//          |  id String @id @default(cuid())
//          |  title: String @db(name: "my_column")
//          |}
//        """.stripMargin
//      }
//      database.setup(project)
//
//      val result = server.query(
//        s"""{
//           |  todo(where: {id: "5beea4aa6183dd734b2dbd9b"}){
//           |    ...todoFields
//           |  }
//           |}
//           |
//           |fragment todoFields on Todo {
//           |  id
//           |  title
//           |}
//           |""".stripMargin,
//        project
//      )
//
//      result.toString should equal("""{"data":{"todo":null}}""")
//    }
  }
}
