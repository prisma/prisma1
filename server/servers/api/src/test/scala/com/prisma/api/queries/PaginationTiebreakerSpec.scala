package com.prisma.api.queries

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class PaginationTiebreakerSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromStringV11() {
    """type User {
      |  id: ID! @id
      |  numFollowers: Int!
      |  pos: Int!
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
    createData()
  }

  //region After

  "After with ties and default order " should "work" in {
    val initial = server.query("""{users{numFollowers, pos}}""", project)

    initial.toString() should be(
      """{"data":{"users":[{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3},{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")

    val after = server
      .query(
        """
          |query {
          |  users(  where: {pos: 3}) {
          |    id
          |    numFollowers
          |    pos
          |  }
          |}
        """,
        project
      )
      .pathAsSeq("data.users")
      .head
      .pathAsString("id")

    val result = server.query(
      s"""
         |{
         |  users(
         |  after: "$after"
         |  ){numFollowers, pos}
         |}
      """,
      project
    )

    result.toString() should be("""{"data":{"users":[{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")
  }

  "After with ties and specific descending order " should "work" in {
    val initial = server.query("""{users(orderBy: numFollowers_DESC){numFollowers, pos}}""", project)

    initial.toString() should be(
      """{"data":{"users":[{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6},{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3}]}}""")

    val after = server
      .query(
        """
        |query {
        |  users(  where: {pos: 4}) {
        |    id
        |    numFollowers
        |    pos
        |  }
        |}
      """,
        project
      )
      .pathAsSeq("data.users")
      .head
      .pathAsString("id")

    val result = server.query(
      s"""
        |{
        |  users(
        |  orderBy: numFollowers_DESC,
        |  after: "$after"
        |  ){numFollowers, pos}
        |}
      """,
      project
    )

    result.toString() should be(
      """{"data":{"users":[{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6},{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3}]}}""")
  }

  "After with ties and specific ascending order" should "work" in {
    val initial = server.query("""{users(orderBy: numFollowers_ASC){numFollowers, pos}}""", project)

    initial.toString() should be(
      """{"data":{"users":[{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3},{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")

    val after = server
      .query(
        """
          |query {
          |  users(  where: {pos: 2}) {
          |    id
          |    numFollowers
          |    pos
          |  }
          |}
        """,
        project
      )
      .pathAsSeq("data.users")
      .head
      .pathAsString("id")

    val result = server.query(
      s"""
         |{
         |  users(
         |  orderBy: numFollowers_ASC,
         |  after: "$after"
         |  ){numFollowers, pos}
         |}
      """,
      project
    )

    result.toString() should be(
      """{"data":{"users":[{"numFollowers":9,"pos":3},{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")
  }

  //endregion

  //region Before

  "Before with ties and default order " should "work" in {
    val initial = server.query("""{users{numFollowers, pos}}""", project)

    initial.toString() should be(
      """{"data":{"users":[{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3},{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")

    val before = server
      .query(
        """
          |query {
          |  users(  where: {pos: 4}) {
          |    id
          |    numFollowers
          |    pos
          |  }
          |}
        """,
        project
      )
      .pathAsSeq("data.users")
      .head
      .pathAsString("id")

    val result = server.query(
      s"""
         |{
         |  users(
         |  before: "$before"
         |  ){numFollowers, pos}
         |}
      """,
      project
    )

    result.toString() should be("""{"data":{"users":[{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3}]}}""")
  }

  "Before with ties and specific descending order " should "work" in {
    val initial = server.query("""{users(orderBy: numFollowers_DESC){numFollowers, pos}}""", project)

    initial.toString() should be(
      """{"data":{"users":[{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6},{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3}]}}""")

    val before = server
      .query(
        """
          |query {
          |  users(  where: {pos: 1}) {
          |    id
          |    numFollowers
          |    pos
          |  }
          |}
        """,
        project
      )
      .pathAsSeq("data.users")
      .head
      .pathAsString("id")

    val result = server.query(
      s"""
         |{
         |  users(
         |  orderBy: numFollowers_DESC,
         |  before: "$before"
         |  ){numFollowers, pos}
         |}
      """,
      project
    )

    result.toString() should be("""{"data":{"users":[{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")
  }

  "Before with ties and specific ascending order" should "work" in {
    val initial = server.query("""{users(orderBy: numFollowers_ASC){numFollowers, pos}}""", project)

    initial.toString() should be(
      """{"data":{"users":[{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2},{"numFollowers":9,"pos":3},{"numFollowers":10,"pos":4},{"numFollowers":10,"pos":5},{"numFollowers":10,"pos":6}]}}""")

    val before = server
      .query(
        """
          |query {
          |  users(  where: {pos: 3}) {
          |    id
          |    numFollowers
          |    pos
          |  }
          |}
        """,
        project
      )
      .pathAsSeq("data.users")
      .head
      .pathAsString("id")

    val result = server.query(
      s"""
         |{
         |  users(
         |  orderBy: numFollowers_ASC,
         |  before: "$before"
         |  ){numFollowers, pos}
         |}
      """,
      project
    )

    result.toString() should be("""{"data":{"users":[{"numFollowers":9,"pos":1},{"numFollowers":9,"pos":2}]}}""")
  }

  //endregion

  private def createData(): Unit = {
    server.query(
      """
        |mutation {
        |  a: createUser(data: {numFollowers: 9, pos: 1}) { id }
        |  b: createUser(data: {numFollowers: 9, pos: 2}) { id }
        |  c: createUser(data: {numFollowers: 9, pos: 3}) { id }
        |  e: createUser(data: {numFollowers: 10, pos: 4}) { id }
        |  f: createUser(data: {numFollowers: 10, pos: 5}) { id }
        |  d: createUser(data: {numFollowers: 10, pos: 6}) { id }
        |}
      """,
      project
    )
  }
}
