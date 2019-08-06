package writes.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class RelationDesignSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val schema =
    """model List{
        |   id    String  @id @default(cuid())
        |   uList String? @unique
        |   todo  Todo?   @relation(references: [id])
        |}
        |
        |model Todo{
        |   id    String  @id @default(cuid())
        |   uTodo String? @unique
        |   list  List?
        |}"""

  val project = SchemaDsl.fromStringV11() { schema }

  "Deleting a parent node" should "remove it from the relation and delete the relay id" in {
    database.setup(project)

    server.query(s"""mutation {createList(data: {uList: "A", todo : { create: {uTodo: "B"}}}){id}}""", project)

    val result = server.query(s"""query{lists {uList, todo {uTodo}}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","todo":{"uTodo":"B"}}]}}""")

    server.query(s"""query{todoes {uTodo}}""", project).toString should be("""{"data":{"todoes":[{"uTodo":"B"}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(1)

    server.query(s"""mutation{deleteList(where: {uList:"A"}){id}}""", project)

    countItems(project, "lists") should be(0)

  }

  "Deleting a child node" should "remove it from the relation and delete the relay id" in {
    database.setup(project)

    server.query(s"""mutation {createList(data: {uList: "A", todo : { create: {uTodo: "B"}}}){id}}""", project)

    val result = server.query(s"""query{lists {uList, todo {uTodo}}}""", project)
    result.toString should equal("""{"data":{"lists":[{"uList":"A","todo":{"uTodo":"B"}}]}}""")

    server.query(s"""query{todoes {uTodo}}""", project).toString should be("""{"data":{"todoes":[{"uTodo":"B"}]}}""")

    countItems(project, "lists") should be(1)
    countItems(project, "todoes") should be(1)

    server.query(s"""mutation{deleteTodo(where: {uTodo:"B"}){id}}""", project)

    countItems(project, "todoes") should be(0)

  }

  def countItems(project: Project, name: String): Int = {
    server.query(s"""query{$name{id}}""", project).pathAsSeq(s"data.$name").length
  }

}
