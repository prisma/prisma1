package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpsertMutationSpec extends FlatSpec with Matchers with ApiBaseSpec {
  val project = SchemaDsl() { schema =>
    schema.model("Todo").field_!("title", _.String).field_!("alias", _.String, isUnique = true)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncate(project)
  }

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

  "an item" should "be updated if it already exists (by id)" in {
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

  "an item" should "be updated if it already exists (by any unique argument)" in {
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

  "[BUG DOC] an upsert" should "perform a create and an update if the update changes the unique field used in the where clause" in {
    val todoId = server
      .executeQuerySimple(
        """mutation {
          |  createTodo(
          |    data: {
          |      title: "title"
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
         |    where: {alias: "todo1"}
         |    create: {
         |      title: "title of new node"
         |      alias: "alias-of-new-node"
         |    }
         |    update: {
         |      title: "updated title"
         |      alias: "todo1-new"
         |    }
         |  ){
         |    id
         |    title
         |  }
         |}
      """.stripMargin,
      project
    )

    // the mutation returns new created node
    result.pathAsString("data.upsertTodo.title") should equal("title of new node")
    // there are 2 nodes. So the create must have been performed.
    todoCount should be(2)
    // the original node has been updated
    server
      .executeQuerySimple(
        s"""{
        |  todo(where: {id: "$todoId"}){
        |    title
        |  }
        |}
      """.stripMargin,
        project
      )
      .pathAsString("data.todo.title") should equal("updated title")
    // a new node has been added
    server
      .executeQuerySimple(
        s"""{
           |  todo(where: {alias: "alias-of-new-node"}){
           |    title
           |  }
           |}
      """.stripMargin,
        project
      )
      .pathAsString("data.todo.title") should equal("title of new node")
  }

  def todoCount: Int = {
    val result = server.executeQuerySimple(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size
  }
}
