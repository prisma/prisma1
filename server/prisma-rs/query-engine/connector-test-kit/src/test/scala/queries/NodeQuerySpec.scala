package queries

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.{JoinRelationLinksCapability, NodeQueryCapability}
import util._

class NodeQuerySpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(NodeQueryCapability, JoinRelationLinksCapability)

  "the node query" should "return null if the id does not exist" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |model Todo {
        |  id String @id @default(cuid())
        |  title: String!
        |}
      """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      s"""{
         |  node(id: "5bedb10fe11dc97034b2390c"){
         |    id
         |    ... on Todo {
         |      title
         |    }
         |  }
         |}""".stripMargin,
      project
    )

    result.toString should equal("""{"data":{"node":null}}""")
  }

  "the node query" should "work if the given id exists" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |model Todo {
        |  id String @id @default(cuid())
        |  title: String!
        |}
      """.stripMargin
    }
    database.setup(project)

    val title = "Hello World!"
    val id = server
      .query(
        s"""mutation {
        |  createTodo(data: {title: "$title"}) {
        |    id
        |  }
        |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    val result = server.query(
      s"""{
        |  node(id: "$id"){
        |    id
        |    ... on Todo {
        |      title
        |    }
        |  }
        |}""".stripMargin,
      project
    )

    result.pathAsString("data.node.title") should equal(title)
  }

  "the node query" should "work if the given id exists when using multiple complicated fragments" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |model Todo {
        |  id String @id @default(cuid())
        |  title: String!
        |  todo: Todo @relation(name:"TodoSelfRelation1" references: [id])
        |  todo2: Todo @relation(name:"TodoSelfRelation2" references: [id])
        |  comment: Comment @relation(name:"TodoToComment1" references: [id])
        |}
        |
        |model Comment {
        |  id String @id @default(cuid())
        |  text1: String!
        |  text2: String!
        |  text3: String!
        |}
      """.stripMargin
    }
    database.setup(project)

    val title = "Hello World!"

    val todo1id = server
      .query(
        s"""mutation {
           |  createTodo(data: {
           |    title: "todo1"
           |    comment:  {create :{text1:"text1" text2: "text2" text3:"text3"}}
           |  }) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    val id = server
      .query(
        s"""mutation {
           |  createTodo(data: {
           |    title: "$title"
           |    todo: {
           |      connect: {id: "$todo1id"}
           |    }
           |    todo2: {
           |      connect: {id: "$todo1id"}
           |    }
           |  }) {
           |    id
           |  }
           |}""".stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    /**
      * In total there are 3 selections for the model Comment. They don't select the same fields.
      * The structure is so complicated to ensure we test the following thing:
      * When the comment gets resolved it is based upon 2 deferred values. The first deferred value contains the selected field 'text1'.
      * The second deferred value contains the selected fields 'text2' and 'text3'. This test makes sure that the deferred value resolution retrieves all 3 fields
      * from the database.
      * */
    val result = server.query(
      s"""{
         |  node(id: "$id"){
         |    id
         |    ...on Todo {
         |      title
         |      todo {
         |        comment {
         |          text1
         |        }
         |      }
         |      todo2 {
         |        comment {
         |          text2
         |        }
         |        ...todoFields
         |      }
         |    }
         |  }
         |}
         |
         |fragment todoFields on Todo {
         |  comment {
         |    text3
         |  }
         |}
         |""".stripMargin,
      project
    )

    result.pathAsString("data.node.title") should equal(title)
    result.pathAsString("data.node.todo.comment.text1") should equal("text1")
    result.pathAsString("data.node.todo2.comment.text2") should equal("text2")
    result.pathAsString("data.node.todo2.comment.text3") should equal("text3")
  }
}
