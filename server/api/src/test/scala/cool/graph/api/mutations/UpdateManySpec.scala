package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateManySpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl() { schema =>
    schema.model("Todo").field_!("title", _.String)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncate(project)
  }

  "The update items Mutation" should "update the items matching the where clause" in {
    createTodo("title")

    val result = server.executeQuerySimple(
      """mutation {
        |  updateTodoes(
        |    where: { title: "title" }
        |    data: { title: "updated title" }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.updateTodoes.count") should equal(1)

    val todoes = server.executeQuerySimple(
      """{
        |  todoes {
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      todoes.pathAsJsValue("data.todoes").toString,
      """[{"title":"updated title"}]"""
    )
  }

  "The update items Mutation" should "update all items if the where clause is empty" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")

    val result = server.executeQuerySimple(
      """mutation {
        |  updateTodoes(
        |    where: { }
        |    data: { title: "updated title" }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.updateTodoes.count") should equal(3)

    val todoes = server.executeQuerySimple(
      """{
        |  todoes {
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      todoes.pathAsJsValue("data.todoes").toString,
      """[{"title":"updated title"},{"title":"updated title"},{"title":"updated title"}]"""
    )

  }

  def createTodo(title: String): Unit = {
    server.executeQuerySimple(
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
  }
}
