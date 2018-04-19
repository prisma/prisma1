package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.connector.postgresql.database.PostgresApiDatabaseQueryBuilder
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteManySpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl() { schema =>
    schema.model("Todo").field_!("title", _.String)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "The delete many Mutation" should "delete the items matching the where clause" in {
    createTodo("title1")
    createTodo("title2")
    todoAndRelayCountShouldBe(2)

    val result = server.query(
      """mutation {
        |  deleteManyTodoes(
        |    where: { title: "title1" }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.deleteManyTodoes.count") should equal(1)

    todoAndRelayCountShouldBe(1)
  }

  "The delete many Mutation" should "delete all items if the where clause is empty" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")
    todoAndRelayCountShouldBe(3)

    val result = server.query(
      """mutation {
        |  deleteManyTodoes(
        |    where: { }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.deleteManyTodoes.count") should equal(3)

    todoAndRelayCountShouldBe(0)
  }

  "The delete many Mutation" should "delete all items using in" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")

    val result = server.query(
      """mutation {
        |  deleteManyTodoes(
        |    where: { title_in: [ "title1", "title2" ]}
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.deleteManyTodoes.count") should equal(2)

    todoAndRelayCountShouldBe(1)

  }

  "The delete many Mutation" should "delete all items using notin" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")

    val result = server.query(
      """mutation {
        |  deleteManyTodoes(
        |    where: { title_not_in: [ "DoesNotExist", "AlsoDoesntExist" ]}
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.deleteManyTodoes.count") should equal(3)

    todoAndRelayCountShouldBe(0)
  }

  "The delete many Mutation" should "delete items using  OR" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")

    val query = server.query(
      """query {
        |  todoes(
        |    where: { OR: [{title: "title1"}, {title: "title2"}]}
        |  ){
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )

    query.toString should be("""{"data":{"todoes":[{"title":"title1"},{"title":"title2"}]}}""")

    val result = server.query(
      """mutation {
        |  deleteManyTodoes(
        |    where: { OR: [{title: "title1"}, {title: "title2"}]}
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.deleteManyTodoes.count") should equal(2)

    todoAndRelayCountShouldBe(1)
  }

  "The delete many Mutation" should "delete items using  AND" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")

    val query = server.query(
      """query {
        |  todoes(
        |    where: { AND: [{title: "title1"}, {title: "title2"}]}
        |  ){
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )

    query.toString should be("""{"data":{"todoes":[]}}""")

    val result = server.query(
      """mutation {
        |  deleteManyTodoes(
        |    where: { AND: [{title: "title1"}, {title: "title2"}]}
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.deleteManyTodoes.count") should equal(0)

    todoAndRelayCountShouldBe(3)
  }

  def todoCount: Int = {
    val result = server.query(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size
  }

  def todoAndRelayCountShouldBe(int: Int) = {
    val result = server.query(
      "{ todoes { id } }",
      project
    )
    result.pathAsSeq("data.todoes").size should be(int)

    dataResolver(project).countByTable("_RelayId").await should be(int)

  }

  def createTodo(title: String): Unit = {
    server.query(
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
