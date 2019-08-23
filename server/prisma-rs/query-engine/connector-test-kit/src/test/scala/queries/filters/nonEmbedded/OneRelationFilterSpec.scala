package queries.filters.nonEmbedded

import org.scalatest._
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class OneRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = ProjectDsl.fromString {
    """
      |model Blog {
      |   id   String @id @default(cuid())
      |   name String
      |   post Post?
      |}
      |
      |model Post {
      |   id         String @id @default(cuid())
      |   title      String
      |   popularity Int
      |   blog       Blog?    @relation(references: [id])
      |   comment    Comment?
      |}
      |
      |model Comment {
      |   id    String  @id @default(cuid())
      |   text  String
      |   likes Int
      |   post  Post?   @relation(references: [id])
      |}
    """
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach() = {
    super.beforeEach()
    database.truncateProjectTables(project)

    // add data
    server.query(
      """mutation {createBlog(
        |     data: {
        |       name: "blog 1",
        |       post:{create: {title: "post 1", popularity: 10, comment:{ create: {text:"comment 1", likes: 10 }}}}
        | }
        |){name}}""",
      project = project
    )

    server.query(
      """mutation {createBlog(data:{
        |                         name: "blog 2",
        |                         post: {create:{title: "post 2",popularity: 100,comment:{create:{text:"comment 2", likes: 100}}}}
        |}){name}}""",
      project = project
    )

    server.query(
      """mutation {createBlog(data:{
        |                         name: "blog 3",
        |                         post: {create:{title: "post 3",popularity: 1000,comment:{create:{text:"comment 3", likes: 1000}}}}
        |}){name}}""",
      project = project
    )

  }

  "Scalar filter" should "work" in {
    server.query(query = """{posts(where:{title: "post 2"}){title}}""", project = project).toString should be("""{"data":{"posts":[{"title":"post 2"}]}}""")
  }

  "1 level 1-relation filter" should "work" in {

    server.query(query = """{posts(where:{blog:{name: "blog 1"}}){title}}""", project = project).toString should be(
      """{"data":{"posts":[{"title":"post 1"}]}}""")

    server.query(query = """{blogs(where:{post:{popularity_gte: 100}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"},{"name":"blog 3"}]}}""")

    server.query(query = """{blogs(where:{post:{popularity_gte: 500}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 3"}]}}""")
  }

  "2 level 1-relation filter" should "work" in {

    server.query(query = """{blogs(where:{post:{comment: {likes: 10}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{post:{comment: {likes: 1000}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 3"}]}}""")

  }

  "crazy filters" should "work" in {

    server
      .query(
        query = """{posts(where: {
                |  blog: {
                |    post: {
                |      popularity_gte: 10
                |    }
                |    name_contains: "blog 1"
                |  }
                |  comment: {
                |    likes_gte: 5
                |    likes_lte: 200
                |  }
                |}) {
                |  title
                |}}""".stripMargin,
        project = project
      )
      .toString should be("""{"data":{"posts":[{"title":"post 1"}]}}""")
  }

  "Join Relation Filter on one to one relation" should "work on one level" in {

    val project = ProjectDsl.fromString {
      """
        |model Post {
        |  id      String @id @default(cuid())
        |  author  AUser?
        |  title   String @unique
        |}
        |
        |model AUser {
        |  id    String  @id @default(cuid())
        |  name  String  @unique
        |  int   Int?
        |  post  Post?   @relation(references: [id])
        |}"""
    }

    database.setup(project)

    server.query(s""" mutation {createPost(data: {title:"Title1"}) {title}} """, project)
    server.query(s""" mutation {createPost(data: {title:"Title2"}) {title}} """, project)
    server.query(s""" mutation {createAUser(data: {name:"Author1", int: 5}) {name}} """, project)
    server.query(s""" mutation {createAUser(data: {name:"Author2", int: 4}) {name}} """, project)

    server.query(s""" mutation {updateAUser(where: { name: "Author1"}, data:{post:{connect:{title: "Title1"}}}) {name}} """, project)
    server.query(s""" mutation {updateAUser(where: { name: "Author2"}, data:{post:{connect:{title: "Title2"}}}) {name}} """, project)

    server.query("""query{aUsers{name, post{title}}}""", project).toString should be(
      """{"data":{"aUsers":[{"name":"Author1","post":{"title":"Title1"}},{"name":"Author2","post":{"title":"Title2"}}]}}""")

    server.query("""query{posts {title, author {name}}}""", project).toString should be(
      """{"data":{"posts":[{"title":"Title1","author":{"name":"Author1"}},{"title":"Title2","author":{"name":"Author2"}}]}}""")

    val res = server.query("""query{aUsers(where:{ post:{title_ends_with: "1"}, name_starts_with: "Author", int: 5}){name, post{title}}}""", project)
    res.toString should be("""{"data":{"aUsers":[{"name":"Author1","post":{"title":"Title1"}}]}}""")
  }
}
