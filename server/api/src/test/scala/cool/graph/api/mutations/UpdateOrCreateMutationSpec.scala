package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateOrCreateMutationSpec extends FlatSpec with Matchers with ApiBaseSpec {
  val project = SchemaDsl() { schema =>
    val comment = schema.model("Comment").field_!("text", _.String)
    schema.model("Todo").field_!("title", _.String).field_!("alias", _.String, isUnique = true).oneToManyRelation("comments", "todo", comment)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncate(project)
  }

  /**
    * create if it doesn't exist yet
    * update if id exists by id
    * update if id exists by any unique
    */
  "an item" should "be created if it does not exist yet" in {
    todoCount should be(0)

    val todoId = "non-existent-id"
    val result = server.executeQuerySimple(
      s"""mutation {
        |  upsertTodo(
        |    where: {id: "$todoId"}
        |    create: {
        |      title: "new title"
        |      alias: "todo1"
        |    }
        |    update: {
        |      title: "updated title"
        |    }
        |  ){
        |    id
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertTodo.title") should be("new title")

    todoCount should be(1)
  }

  "an item" should "be updated if it already exsists (by id)" in {
    val todoId = server
      .executeQuerySimple(
        """mutation {
        |  createTodo(
        |    data: {
        |      title: "new title1"
        |      alias: "todo1"
        |    }
        |  ) {
        |    id
        |  }
        |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    todoCount should be(1)

    val result = server.executeQuerySimple(
      s"""mutation {
         |  upsertTodo(
         |    where: {id: "$todoId"}
         |    create: {
         |      title: "irrelevant"
         |      alias: "irrelevant"
         |    }
         |    update: {
         |      title: "updated title"
         |    }
         |  ){
         |    id
         |    title
         |  }
         |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertTodo.title") should be("updated title")

    todoCount should be(1)
  }

  "an item" should "be updated if it already exsists (by any unique argument)" in {
    val todoAlias = server
      .executeQuerySimple(
        """mutation {
          |  createTodo(
          |    data: {
          |      title: "new title1"
          |      alias: "todo1"
          |    }
          |  ) {
          |    alias
          |  }
          |}
        """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.alias")

    todoCount should be(1)

    val result = server.executeQuerySimple(
      s"""mutation {
         |  upsertTodo(
         |    where: {alias: "$todoAlias"}
         |    create: {
         |      title: "irrelevant"
         |      alias: "irrelevant"
         |    }
         |    update: {
         |      title: "updated title"
         |    }
         |  ){
         |    id
         |    title
         |  }
         |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertTodo.title") should be("updated title")

    todoCount should be(1)
  }

  def todoCount: Int = {
    val result = server.executeQuerySimple(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size
  }
}
