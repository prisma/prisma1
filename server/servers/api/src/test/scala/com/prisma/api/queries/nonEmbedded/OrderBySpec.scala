package com.prisma.api.queries.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.ApiConnectorCapability.RelationsCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class OrderBySpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(RelationsCapability)

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
      |  lists: [List!]!
      |}
      |
      |type NeedsTiebreaker {
      |  id: ID! @unique
      |  name: String!
      |  order: Int!
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    createLists()
    createTodos()
    createNeedsTiebreakers()
  }

  "The order when not giving an order by" should "be by Id ascending and therefore oldest first" in {
    val resultWithOrderByImplicitlySpecified = server.query(
      """
        |{
        |  needsTiebreakers {
        |    order
        |  }
        |}
      """,
      project
    )

    resultWithOrderByImplicitlySpecified.toString should be(
      """{"data":{"needsTiebreakers":[{"order":1},{"order":2},{"order":3},{"order":4},{"order":5},{"order":6},{"order":7}]}}""")

    val resultWithOrderByExplicitlySpecified = server.query(
      """
        |{
        |  needsTiebreakers(orderBy: id_ASC) {
        |    order
        |  }
        |}
      """,
      project
    )
    resultWithOrderByImplicitlySpecified should be(resultWithOrderByExplicitlySpecified)
  }

  "The order when not giving an order by and using last" should "be by Id ascending and therefore oldest first" in {
    val result = server.query(
      """
        |{
        |  needsTiebreakers(last: 3) {
        |    order
        |  }
        |}
      """,
      project
    )

    result.toString should be("""{"data":{"needsTiebreakers":[{"order":5},{"order":6},{"order":7}]}}""")
  }

  "The order when giving an order by ASC that only has ties" should "be by Id ascending and therefore oldest first" in {
    val result = server.query(
      """
        |{
        |  needsTiebreakers(orderBy: name_ASC) {
        |    order
        |  }
        |}
      """,
      project
    )

    result.toString should be("""{"data":{"needsTiebreakers":[{"order":1},{"order":2},{"order":3},{"order":4},{"order":5},{"order":6},{"order":7}]}}""")
  }

  "The order when giving an order by ASC that only has ties and uses last" should "be by Id ascending and therefore oldest first" in {
    val result = server.query(
      """
        |{
        |  needsTiebreakers(orderBy: name_ASC, last: 3) {
        |    order
        |  }
        |}
      """,
      project
    )

    result.toString should be("""{"data":{"needsTiebreakers":[{"order":5},{"order":6},{"order":7}]}}""")
  }

  "The order when giving an order by DESC that only has ties" should "be by Id ascending and therefore oldest first" in {
    val result = server.query(
      """
        |{
        |  needsTiebreakers(orderBy: name_DESC) {
        |    order
        |  }
        |}
      """,
      project
    )

    result.toString should be("""{"data":{"needsTiebreakers":[{"order":1},{"order":2},{"order":3},{"order":4},{"order":5},{"order":6},{"order":7}]}}""")
  }

  "The order when giving an order by DESC that only has ties and uses last" should "be by Id ascending and therefore oldest first" in {
    val result = server.query(
      """
        |{
        |  needsTiebreakers(orderBy: name_DESC, last: 3) {
        |    order
        |  }
        |}
      """,
      project
    )

    result.toString should be("""{"data":{"needsTiebreakers":[{"order":5},{"order":6},{"order":7}]}}""")
  }

  "The order when not using order by" should "be the same no matter if pagination is used or not" in {
    val result1 = server.query(
      """
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    todos{
        |      title
        |    }
        |  }
        |}
      """,
      project
    )

    val result2 = server.query(
      """
        |{
        |  list(where: {name: "1"}) {
        |    name
        |    todos(first:10){
        |      title
        |    }
        |  }
        |}
      """,
      project
    )

    result1 should be(result2)

    val result3 = server.query(
      """
        |{
        |  todo(where: {title: "1"}) {
        |    title
        |    lists{
        |      name
        |    }
        |  }
        |}
      """,
      project
    )

    val result4 = server.query(
      """
        |{
        |  todo(where: {title: "1"}) {
        |    title
        |    lists(first:10){
        |      name
        |    }
        |  }
        |}
      """,
      project
    )

    result3 should be(result4)
  }

  private def createLists(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createList(data: {name: "1"}){ id }
        |  b: createList(data: {name: "2"}){ id }
        |  d: createList(data: {name: "4"}){ id }
        |  f: createList(data: {name: "6"}){ id }
        |  g: createList(data: {name: "7"}){ id }
        |  c: createList(data: {name: "3"}){ id }
        |  e: createList(data: {name: "5"}){ id }
        |}
      """,
      project
    )
  }

  private def createTodos(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createTodo(data: {title: "1", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |  c: createTodo(data: {title: "3", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |  d: createTodo(data: {title: "4", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |  f: createTodo(data: {title: "6", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |  g: createTodo(data: {title: "7", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |  b: createTodo(data: {title: "2", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |  e: createTodo(data: {title: "5", lists: { connect: [{name:"1"},{name:"2"},{name:"3"}]}}){ id }
        |}
      """,
      project
    )
  }

  private def createNeedsTiebreakers(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createNeedsTiebreaker(data: {name: "SameSame", order: 1}){ id }
        |  b: createNeedsTiebreaker(data: {name: "SameSame", order: 2}){ id }
        |  c: createNeedsTiebreaker(data: {name: "SameSame", order: 3}){ id }
        |  d: createNeedsTiebreaker(data: {name: "SameSame", order: 4}){ id }
        |  e: createNeedsTiebreaker(data: {name: "SameSame", order: 5}){ id }
        |  f: createNeedsTiebreaker(data: {name: "SameSame", order: 6}){ id }
        |  g: createNeedsTiebreaker(data: {name: "SameSame", order: 7}){ id }
        |}
      """,
      project
    )
  }
}
