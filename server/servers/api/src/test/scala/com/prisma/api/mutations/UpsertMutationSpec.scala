package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.gc_values.StringGCValue
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpsertMutationSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = SchemaDsl.fromStringV11() {
    """
      |type Todo {
      |  id: ID! @id
      |  title: String!
      |  alias: String! @unique
      |  anotherIDField: ID @unique
      |}
      |
      |type WithDefaultValue {
      |  id: ID! @id
      |  reqString: String! @default(value: "defaultValue")
      |  title: String!
      |}
      |
      |type MultipleFields {
      |  id: ID! @id
      |  reqString: String!
      |  reqInt: Int!
      |  reqFloat: Float!
      |  reqBoolean: Boolean!
      |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "an item" should "be created if it does not exist yet" in {
    todoCount should be(0)

    val todoId = "5beea4aa6183dd734b2dbd9b"
    val result = server.query(
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
      """,
      project
    )

    result.pathAsString("data.upsertTodo.title") should be("new title")

    todoCount should be(1)
  }

  "an item" should "be created with multiple fields of different types" in {
    todoCount should be(0)

    val id = "5beea4aa6183dd734b2dbd9b"
    val result = server.query(
      s"""mutation {
         |  upsertMultipleFields(
         |    where: {id: "$id"}
         |    create: {
         |      reqString: "new title"
         |      reqInt: 1
         |      reqFloat: 1.22
         |      reqBoolean: true
         |    }
         |    update: {
         |      reqString: "title"
         |      reqInt: 2
         |      reqFloat: 5.223423423423
         |      reqBoolean: false
         |    }
         |  ){
         |    id
         |    reqString
         |    reqInt
         |    reqFloat
         |    reqBoolean
         |  }
         |}
      """,
      project
    )
    result.pathAsString("data.upsertMultipleFields.reqString") should be("new title")
    result.pathAsLong("data.upsertMultipleFields.reqInt") should be(1)
    result.pathAsDouble("data.upsertMultipleFields.reqFloat") should be(1.22)
    result.pathAsBool("data.upsertMultipleFields.reqBoolean") should be(true)

  }

  "an item" should "be created if it does not exist yet and use the defaultValue if necessary" in {
    val todoId = "5beea4aa6183dd734b2dbd9b"
    val result = server.query(
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
         |    title
         |    reqString
         |  }
         |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.upsertWithDefaultValue.title") should be("new title")
    result.pathAsString("data.upsertWithDefaultValue.reqString") should be("defaultValue")
  }

  "an item" should "not be created when trying to set a required value to null even if there is a default value for that field" in {
    server.queryThatMustFail(
      s"""mutation {
         |  upsertWithDefaultValue(
         |    where: {id: "NonExistantID"}
         |    create: {
         |      reqString: null
         |      title: "new title"
         |    }
         |    update: {
         |      title: "updated title"
         |    }
         |  ){
         |    title
         |    reqString
         |  }
         |}
      """.stripMargin,
      project,
      3036,
      errorContains = "The input value null was not valid for field reqString of type WithDefaultValue."
    )
  }

  "an item" should "be updated if it already exists (by id)" in {
    val todoId = server
      .query(
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

    val result = server.query(
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
      .query(
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

    server.queryThatMustFail(
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
      .query(
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

    val result = server.query(
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
      .query(
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

    val result = server.query(
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
      .query(
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
      .query(
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

    val result = server.query(
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
      .query(
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
    val result = server.query(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size
  }
}
