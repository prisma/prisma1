package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.gc_values.StringGCValue
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpsertMutationSpec extends FlatSpec with Matchers with ApiBaseSpec {
  val project: Project = SchemaDsl() { schema =>
    schema.model("Todo").field_!("title", _.String).field_!("alias", _.String, isUnique = true).field("anotherIDField", _.GraphQLID, isUnique = true)
    schema.model("WithDefaultValue").field("default", _.String, defaultValue = Some(StringGCValue("defaultValue"))).field_!("title", _.String)
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

  "an item" should "be created if it does not exist yet and use the defaultValue if necessary" in {
    todoCount should be(0)

    val todoId = "non-existent-id"
    val result = server.executeQuerySimple(
      s"""mutation {
         |  upsertWithDefaultValue(
         |    where: {id: "$todoId"}
         |    create: {
         |      title: "new title"
         |    }
         |    update: {
         |      title: "updated title"
         |    }
         |  ){
         |    id
         |    title
         |    default
         |  }
         |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertWithDefaultValue.title") should be("new title")
    result.pathAsString("data.upsertWithDefaultValue.default") should be("defaultValue")

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

    server.executeQuerySimpleThatMustFail(
      s"""mutation {
         |  upsertTodo(
         |    where: {alias: "$todoAlias"}
         |    create: {
         |      title: "irrelevant"
         |      alias: "irrelevant"
         |      anotherIDField: "morethantwentyfivecharacterslong"
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
      project,
      3007
    )
  }

  "Inputvaluevalidations" should "fire if an ID is too long" in {
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

  "An upsert" should "perform only an update if the update changes the unique field used in the where clause" in {
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

    result.pathAsString("data.upsertTodo.title") should equal("updated title")
    todoCount should be(1)
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
  }

  "An upsert" should "perform only an update if the update changes nothing" in {
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
         |      title: "title"
         |      alias: "todo1"
         |    }
         |  ){
         |    id
         |    title
         |  }
         |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertTodo.title") should equal("title")
    todoCount should be(1)
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
      .pathAsString("data.todo.title") should equal("title")
  }

  def todoCount: Int = {
    val result = server.executeQuerySimple(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size
  }
}
