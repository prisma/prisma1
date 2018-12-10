package com.prisma.api.filters.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class OneRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromString() {
    """
      |type Blog {
      |   id: ID! @unique
      |   name: String!
      |   post: Post
      |}
      |
      |type Post {
      |   id: ID! @unique
      |   title: String!
      |   popularity: Int!
      |   blog: Blog
      |   comment: Comment
      |}
      |
      |type Comment {
      |   id: ID! @unique
      |   text: String!
      |   likes: Int!
      |   post: Post
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
}
