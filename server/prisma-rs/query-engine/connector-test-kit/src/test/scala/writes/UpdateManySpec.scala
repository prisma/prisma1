package writes

import org.scalatest.{FlatSpec, Matchers}
import util._

class UpdateManySpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = ProjectDsl.fromString {
    """model Todo {
      |  id     String  @id @default(cuid())
      |  title  String
      |  opt    String?
      |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }
  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "The update items Mutation" should "update the items matching the where clause" in {
    createTodo("title1")
    createTodo("title2")

    val result = server.query(
      """mutation {
        |  updateManyTodoes(
        |    where: { title: "title1" }
        |    data: { title: "updated title", opt: "test" }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.updateManyTodoes.count") should equal(1)

    val todoes = server.query(
      """{
        |  todoes {
        |    title
        |    opt
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      todoes.pathAsJsValue("data.todoes").toString,
      """[{"title":"updated title","opt":"test"},{"title":"title2","opt":null}]"""
    )
  }

  "The update items Mutation" should "update all items if the where clause is empty" in {
    createTodo("title1")
    createTodo("title2")
    createTodo("title3")

    val result = server.query(
      """mutation {
        |  updateManyTodoes(
        |    where: { }
        |    data: { title: "updated title" }
        |  ){
        |    count
        |  }
        |}
      """.stripMargin,
      project
    )
    result.pathAsLong("data.updateManyTodoes.count") should equal(3)

    val todoes = server.query(
      """{
        |  todoes {
        |    title
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      todoes.pathAsJsValue("data.todoes").toString,
      """[{"title":"updated title"},{"title":"updated title"},{"title":"updated title"}]"""
    )
  }

  "UpdateMany" should "work between top level types" in {

    val project = ProjectDsl.fromString {
      """
        |model ZChild{
        |    id      String  @id @default(cuid())
        |    name    String? @unique
        |    test    String?
        |    parent  Parent? @relation(references: [id])
        |}
        |
        |model Parent{
        |    id       String   @id @default(cuid())
        |    name     String?  @unique
        |    children ZChild[]
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[{ name: "Daughter"},{ name: "Daughter2"}, { name: "Son"},{ name: "Son2"}]}
         |}){
         |  name,
         |  children{ name}
         |}}""",
      project
    )

    create.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter"},{"name":"Daughter2"},{"name":"Son"},{"name":"Son2"}]}}}""")

    val nestedUpdateMany = server.query(
      s"""mutation {
         |   updateParent(
         |   where: { name: "Dad" }
         |   data: {  children: {updateMany:[
         |      {
         |          where:{name_contains:"Daughter"}
         |          data:{test: "UpdateManyDaughters"}
         |      },
         |      {
         |          where:{name_contains:"Son"}
         |          data:{test: "UpdateManySons"}
         |      }
         |   ]
         |  }}
         |){
         |  name,
         |  children{ name, test}
         |}}""",
      project
    )

    nestedUpdateMany.toString should be(
      """{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter","test":"UpdateManyDaughters"},{"name":"Daughter2","test":"UpdateManyDaughters"},{"name":"Son","test":"UpdateManySons"},{"name":"Son2","test":"UpdateManySons"}]}}}""")
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
