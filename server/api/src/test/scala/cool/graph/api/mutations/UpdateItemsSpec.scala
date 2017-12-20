package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateItemsSpec extends FlatSpec with Matchers with ApiBaseSpec {

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

  "The update items Mutation" should "update the items matching the where cluase" in {
    server.executeQuerySimple(
      """mutation {
          |  createTodo(
          |    data: {
          |      title: "new title1"
          |    }
          |  ) {
          |    id
          |  }
          |}
        """.stripMargin,
      project
    )
    todoCount should be(1)

    val result = server.executeQuerySimple(
      """mutation {
        |  updateTodoes(
        |    where: { title: "new title1" }
        |    data: { title: "updated title" }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.updateTodoes.count") should equal(1)
  }

  def todoCount: Int = {
    val result = server.executeQuerySimple(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size
  }
}
