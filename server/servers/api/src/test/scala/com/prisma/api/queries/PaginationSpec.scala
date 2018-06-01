package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.api.import_export.BulkImport
import com.prisma.shared.schema_dsl.SchemaDsl
import cool.graph.cuid.Cuid
import org.scalatest.{FlatSpec, Matchers}

class PaginationSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = SchemaDsl.fromString() {
    """
      |type List {
      |  id: ID! @unique
      |  name: String! @unique
      |  todos: [Todo!]!
      |}
      |
      |type Todo {
      |  id: ID! @unique
      |  title: String! @unique
      |  list: List!
      |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    createLists()
    createTodos()
  }

  "hasNextPage" should "be true if there are more nodes" in {
    val result1 = server.query(
      """
        |{
        |  listsConnection(first: 3) {
        |    pageInfo {
        |      hasNextPage
        |      hasPreviousPage
        |      startCursor
        |      endCursor
        |    }
        |    edges {
        |      node {
        |        name
        |      }
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    result1.pathAsJsArray("data.listsConnection.edges").value should have(size(3))
    result1.pathAsBool("data.listsConnection.pageInfo.hasNextPage") should be(true)
    result1.pathAsJsArray("data.listsConnection.edges").toString should equal("""[{"node":{"name":"1"}},{"node":{"name":"2"}},{"node":{"name":"3"}}]""")

    val result2 = server.query(
      """
        |{
        |  listsConnection(skip: 3, first: 3) {
        |    pageInfo {
        |      hasNextPage
        |      hasPreviousPage
        |      startCursor
        |      endCursor
        |    }
        |    edges {
        |      node {
        |        name
        |      }
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    result2.pathAsJsArray("data.listsConnection.edges").value should have(size(3))
    result2.pathAsBool("data.listsConnection.pageInfo.hasNextPage") should be(true)
    result2.pathAsJsArray("data.listsConnection.edges").toString should equal("""[{"node":{"name":"4"}},{"node":{"name":"5"}},{"node":{"name":"6"}}]""")
  }

  "the cursor returned on the top level" should "work" in {
    val result1 = server.query(
      """
        |{
        |  listsConnection(first: 3) {
        |    pageInfo {
        |      hasNextPage
        |      hasPreviousPage
        |      startCursor
        |      endCursor
        |    }
        |    edges {
        |      node {
        |        name
        |      }
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    result1.pathAsJsArray("data.listsConnection.edges").toString should equal("""[{"node":{"name":"1"}},{"node":{"name":"2"}},{"node":{"name":"3"}}]""")
    val cursor = result1.pathAsString("data.listsConnection.pageInfo.endCursor")

    val result2 = server.query(
      s"""
        |{
        |  listsConnection(after: "$cursor", first: 3) {
        |    pageInfo {
        |      hasNextPage
        |      hasPreviousPage
        |      startCursor
        |      endCursor
        |    }
        |    edges {
        |      node {
        |        name
        |      }
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    result2.pathAsJsArray("data.listsConnection.edges").toString should equal("""[{"node":{"name":"4"}},{"node":{"name":"5"}},{"node":{"name":"6"}}]""")
  }

  "the cursor returned on the sub level" should "work" in {
    val result1 = server.query(
      """
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    todos(first: 3){
        |      id
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    result1.pathAsJsArray("data.list.todos").value.map(_.pathAsString("title")) should equal(List("1", "2", "3"))
    val cursor = result1.pathAsString("data.list.todos.[2].id")

    val result2 = server.query(
      s"""
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    todos(after: "$cursor", first: 3){
        |      id
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    result2.pathAsJsArray("data.list.todos").value.map(_.pathAsString("title")) should equal(List("4", "5", "6"))
  }

  private def createLists(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createList(data: {name: "1"}){ id }
        |  b: createList(data: {name: "2"}){ id }
        |  c: createList(data: {name: "3"}){ id }
        |  d: createList(data: {name: "4"}){ id }
        |  e: createList(data: {name: "5"}){ id }
        |  f: createList(data: {name: "6"}){ id }
        |  g: createList(data: {name: "7"}){ id }
        |}
      """.stripMargin,
      project
    )
  }

  private def createTodos(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createTodo(data: {title: "1", list: { connect: {name:"1"}}}){ id }
        |  b: createTodo(data: {title: "2", list: { connect: {name:"1"}}}){ id }
        |  c: createTodo(data: {title: "3", list: { connect: {name:"1"}}}){ id }
        |  d: createTodo(data: {title: "4", list: { connect: {name:"1"}}}){ id }
        |  e: createTodo(data: {title: "5", list: { connect: {name:"1"}}}){ id }
        |  f: createTodo(data: {title: "6", list: { connect: {name:"1"}}}){ id }
        |  g: createTodo(data: {title: "7", list: { connect: {name:"1"}}}){ id }
        |}
      """.stripMargin,
      project
    )
  }
}
