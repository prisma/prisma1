package writes

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.UuidIdCapability
import util._

class DeleteSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = ProjectDsl.fromString {
    """
      |model Todo {
      |  id    String @id @default(cuid())
      |  title String @unique
      |}
    """.stripMargin
  }

  "The delete mutation" should "delete the item matching the where clause" in {
    database.setup(project)
    createTodo(project, "title1")
    createTodo(project, "title2")
    todoCountShouldBe(project, 2)

    server.query(
      """mutation {
        |  deleteTodo(
        |    where: { title: "title1" }
        |  ){
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )

    todoCountShouldBe(project, 1)
  }

  "The delete  mutation" should "error if the where clause misses" in {
    database.setup(project)
    createTodo(project, "title1")
    createTodo(project, "title2")
    createTodo(project, "title3")
    todoCountShouldBe(project, 3)

    server.queryThatMustFail(
      """mutation {
        |  deleteTodo(
        |    where: { title: "does not exist" }
        |  ){
        |    id
        |  }
        |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = """No Node for the model Todo with value does not exist for title found."""
    )

    todoCountShouldBe(project, 3)
  }

  "the delete mutation" should "work when node ids are UUIDs" in {
    if (capabilities.has(UuidIdCapability)) {
      val project = ProjectDsl.fromString(s"""
         |model Todo {
         |  id    String @id @default(uuid())
         |  title String
         |}
       """.stripMargin)

      database.setup(project)

      val id = createTodo(project, "1")
      todoCountShouldBe(project, 1)

      server.query(
        s"""mutation {
        |  deleteTodo(where:{id: "$id"}){
        |    id
        |  }
        |}
      """.stripMargin,
        project
      )
      todoCountShouldBe(project, 0)
    }
  }

  def todoCountShouldBe(project: Project, int: Int) = {
    val result = server.query(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size should be(int)

  }

  def createTodo(project: Project, title: String) = {
    server
      .query(
        s"""mutation {
        |  createTodo(
        |    data: {
        |      title: "$title"
        |    }
        |  ) {
        |    id
        |  }
        |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

  }
}
