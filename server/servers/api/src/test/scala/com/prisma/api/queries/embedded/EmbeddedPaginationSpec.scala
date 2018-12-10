package com.prisma.api.queries.embedded

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedPaginationSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(EmbeddedTypesCapability)
  lazy val project = SchemaDsl.fromString() {
    """
      |type List {
      |  id: ID! @unique
      |  name: String! @unique
      |  todos: [Todo]
      |}
      |
      |type Todo @embedded{
      |  title: String! @unique
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    createLists()
  }

  // region TOP LEVEL

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
      """,
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
      """,
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
      """,
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
      """,
      project
    )
    result2.pathAsJsArray("data.listsConnection.edges").toString should equal("""[{"node":{"name":"4"}},{"node":{"name":"5"}},{"node":{"name":"6"}}]""")
  }

  "the cursor returned on the top level" should "work 2" in {
    val result1 = server.query(
      """
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    id
        |  }
        |}
      """,
      project
    )

    val cursor = result1.pathAsString("data.list.id")

    val result2 = server.query(
      s"""
         |{
         |  lists(first: 2 after:"$cursor") {
         |    name
         |  }
         |}
      """,
      project
    )

    result2.toString should be("""{"data":{"lists":[{"name":"2"},{"name":"3"}]}}""")
  }

  "the cursor returned on the top level" should "work 3" in {
    val result1 = server.query(
      """
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    id
        |  }
        |}
      """,
      project
    )

    val cursor = result1.pathAsString("data.list.id")

    val result2 = server.query(
      s"""
         |{
         |  lists(after:"$cursor") {
         |    name
         |  }
         |}
      """,
      project
    )

    result2.toString should be("""{"data":{"lists":[{"name":"2"},{"name":"3"},{"name":"4"},{"name":"5"},{"name":"6"},{"name":"7"}]}}""")
  }

  "skip" should "work" in {
    val result = server.query(
      s"""
         |{
         |  lists(skip:1) {
         |    name
         |  }
         |}
      """,
      project
    )
    result should be("""{"data":{"lists":[{"name":"2"},{"name":"3"},{"name":"4"},{"name":"5"},{"name":"6"},{"name":"7"}]}}""".parseJson)
  }

  "last" should "work" in {
    val result = server.query(
      s"""
         |{
         |  lists(last:3) {
         |    name
         |  }
         |}
      """,
      project
    )
    result should be("""{"data":{"lists":[{"name":"5"},{"name":"6"},{"name":"7"}]}}""".parseJson)
  }

  // endregion

  //region NESTED

  "a where on the sub level" should "work" taggedAs (IgnoreMongo) in {
    val result1 = server.query(
      """
        |{
        |  list(where:{name:"1"}){
        |    name
        |    todos(where:{title: "3"}){
        |      title
        |    }
        |  }
        |}
      """,
      project
    )

    result1.pathAsJsArray("data.list.todos").value.map(_.pathAsString("title")) should equal(List("3"))

  }

  "the cursor returned on the sub level" should "work" taggedAs (IgnoreMongo) in {
    val result1 = server.query(
      """
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    todos(first: 3){
        |      title
        |    }
        |  }
        |}
      """,
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
      """,
      project
    )
    result2.pathAsJsArray("data.list.todos").value.map(_.pathAsString("title")) should equal(List("4", "5", "6"))
  }

  "the cursor returned on the sub level" should "work 2" taggedAs (IgnoreMongo) in {
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
      """,
      project
    )

    result1.pathAsJsArray("data.list.todos").value.map(_.pathAsString("title")) should equal(List("1", "2", "3"))
    val cursor = result1.pathAsString("data.list.todos.[2].id")

    val result2 = server.query(
      s"""
         |{
         |  list(where: {name: "1"}) {
         |    name
         |    todos(after: "$cursor"){
         |      id
         |      title
         |    }
         |  }
         |}
      """,
      project
    )
    result2.pathAsJsArray("data.list.todos").value.map(_.pathAsString("title")) should equal(List("4", "5", "6", "7"))
  }

  "the pagination" should "work on the sub level" taggedAs (IgnoreMongo) in {
    val result1 = server.query(
      """
        |{
        |  lists {
        |    name
        |    todos(first: 3){
        |      title
        |    }
        |  }
        |}
      """,
      project
    )

    result1 should equal("""
        |{"data":{"lists":[
        |  {"name":"1","todos":[{"title":"1"},{"title":"2"},{"title":"3"}]},
        |  {"name":"2","todos":[]},
        |  {"name":"3","todos":[]},
        |  {"name":"4","todos":[]},
        |  {"name":"5","todos":[]},
        |  {"name":"6","todos":[]},
        |  {"name":"7","todos":[]}]
        |}}
      """.stripMargin.parseJson)
  }

  "skip on relations" should "work" taggedAs (IgnoreMongo) in {
    val result = server.query(
      s"""
         |{
         |  list(where: { name: "1" }) {
         |    todos(skip: 1) {
         |      title
         |    }
         |  }
         |}
      """,
      project
    )
    result should be("""{"data":{"list":{"todos":[{"title":"2"},{"title":"3"},{"title":"4"},{"title":"5"},{"title":"6"},{"title":"7"}]}}}""".parseJson)
  }

  //endregion

  private def createLists(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createList(data: {name: "1", todos:{create:[{title: "1"},{title: "2"},{title: "3"},{title: "4"},{title: "5"},{title: "6"},{title: "7"}]}}){ id }
        |  b: createList(data: {name: "2"}){ id }
        |  c: createList(data: {name: "3"}){ id }
        |  d: createList(data: {name: "4"}){ id }
        |  e: createList(data: {name: "5"}){ id }
        |  f: createList(data: {name: "6"}){ id }
        |  g: createList(data: {name: "7"}){ id }
        |}
      """,
      project
    )
  }
}
