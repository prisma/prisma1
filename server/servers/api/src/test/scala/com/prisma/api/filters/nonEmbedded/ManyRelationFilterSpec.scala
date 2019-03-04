package com.prisma.api.filters.nonEmbedded

import com.prisma.{IgnoreMongo, IgnorePostgres}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class ManyRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromString() {
    """
      |type Blog {
      |   id: ID! @unique
      |   name: String!
      |   posts: [Post]
      |}
      |
      |type Post {
      |   id: ID! @unique
      |   title: String!
      |   popularity: Int!
      |   blog: Blog
      |   comments: [Comment]
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
        |       posts:{
        |         create:[
        |           {title: "post 1", popularity: 10, comments:{
        |                                               create: [{text:"comment 1", likes: 0 },
        |                                                        {text:"comment 2", likes: 5},
        |                                                        {text:"comment 3", likes: 10}]
        |                                             }
        |           },
        |           {title: "post 2", popularity: 2,  comments:{
        |                                               create: [{text:"comment 4", likes: 10}]
        |                                             }
        |           }
        |         ]
        |      }
        | }
        |){name}}""".stripMargin,
      project = project
    )
    server.query(
      """mutation {createBlog(data:{
        |                         name: "blog 2",
        |                         posts: {create: [
        |                                   {title: "post 3",
        |                                    popularity: 1000,
        |                                    comments:{create: [
        |                                             {text:"comment 5", likes: 1000}
        |                                             ]}
        |                                             }]}
        |                                             }){name}}""".stripMargin,
      project = project
    )
  }

  "simple scalar filter" should "work" in {
    server.query(query = """{blogs{posts(where:{popularity_gte: 5}){title}}}""", project = project).toString should be(
      """{"data":{"blogs":[{"posts":[{"title":"post 1"}]},{"posts":[{"title":"post 3"}]}]}}""")
  }

  "1 level 1-relation filter" should "work" in {
    server.query(query = """{posts(where:{blog:{name: "blog 1"}}){title}}""", project = project).toString should be(
      """{"data":{"posts":[{"title":"post 1"},{"title":"post 2"}]}}""")
  }

  "1 level m-relation filter" should "work for _some" in {

    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 5}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 50}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{AND:[{title: "post 1"}, {title: "post 2"}]}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[]}}""")

    server
      .query(query = """{blogs(where:{AND:[{posts_some:{title: "post 1"}}, {posts_some:{title: "post 2"}}]}){name}}""", project = project)
      .toString should be("""{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{AND:[{title: "post 1"}, {popularity_gte: 2}]}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
  }

  "1 level m-relation filter" should "work for _every " taggedAs (IgnoreMongo) in {
    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 2}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 3}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

  "1 level m-relation filter" should "work for _none" taggedAs (IgnoreMongo) in {
    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 50}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 5}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
  }

  "2 level m-relation filter" should "work for some/some" in {

    // some|some
    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 1}}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
  }

  "2 level m-relation filter" should "work for _every, _some and _none" taggedAs (IgnoreMongo) in {
    // some|every
    server.query(query = """{blogs(where:{posts_some:{comments_every: {likes_gte: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{comments_every: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[]}}""")

    // some|none
    server.query(query = """{blogs(where:{posts_some:{comments_none: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{comments_none: {likes_gte: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[]}}""")

    // every|some
    server.query(query = """{blogs(where:{posts_every:{comments_some: {likes: 10}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[]}}""")

    // every|every
    server.query(query = """{blogs(where:{posts_every:{comments_every: {likes_gte: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{comments_every: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[]}}""")

    // every|none
    server.query(query = """{blogs(where:{posts_every:{comments_none: {likes_gte: 100}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{comments_none: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")

    // none|some
    server.query(query = """{blogs(where:{posts_none:{comments_some: {likes_gte: 100}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")

    // none|every
    server.query(query = """{blogs(where:{posts_none:{comments_every: {likes_gte: 11}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{comments_every: {likes_gte: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[]}}""")

    // none|none
    server.query(query = """{blogs(where:{posts_none:{comments_none: {likes_gte: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{comments_none: {likes_gte: 11}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

  "crazy filters" should "work" taggedAs (IgnoreMongo) in {

    server
      .query(
        query = """{posts(where: {
                |  blog: {
                |    posts_some: {
                |      popularity_gte: 5
                |    }
                |    name_contains: "Blog 1"
                |  }
                |  comments_none: {
                |    likes_gte: 5
                |  }
                |  comments_some: {
                |    likes_lte: 2
                |  }
                |}) {
                |  title
                |}}""".stripMargin,
        project = project
      )
      .toString should be("""{"data":{"posts":[]}}""")
  }

  "Join Relation Filter on many to many relation" should "work on one level" taggedAs (IgnorePostgres) in {

    val project = SchemaDsl.fromString() {
      """
        |type Post {
        |  id: ID! @unique
        |  authors: [AUser]
        |  title: String! @unique
        |}
        |
        |type AUser {
        |  id: ID! @unique
        |  name: String! @unique
        |  posts: [Post] @mongoRelation(field: "posts")
        |}"""
    }

    database.setup(project)

    server.query(s""" mutation {createPost(data: {title:"Title1"}) {title}} """, project)
    server.query(s""" mutation {createPost(data: {title:"Title2"}) {title}} """, project)
    server.query(s""" mutation {createAUser(data: {name:"Author1"}) {name}} """, project)
    server.query(s""" mutation {createAUser(data: {name:"Author2"}) {name}} """, project)

    server.query(s""" mutation {updateAUser(where: { name: "Author1"}, data:{posts:{connect:[{title: "Title1"},{title: "Title2"}]}}) {name}} """, project)
    server.query(s""" mutation {updateAUser(where: { name: "Author2"}, data:{posts:{connect:[{title: "Title1"},{title: "Title2"}]}}) {name}} """, project)

    server.query("""query{aUsers{name, posts{title}}}""", project).toString should be(
      """{"data":{"aUsers":[{"name":"Author1","posts":[{"title":"Title1"},{"title":"Title2"}]},{"name":"Author2","posts":[{"title":"Title1"},{"title":"Title2"}]}]}}""")

    server.query("""query{posts {title, authors {name}}}""", project).toString should be(
      """{"data":{"posts":[{"title":"Title1","authors":[{"name":"Author1"},{"name":"Author2"}]},{"title":"Title2","authors":[{"name":"Author1"},{"name":"Author2"}]}]}}""")

    val res = server.query("""query{aUsers(where:{name_starts_with: "Author2", posts_some:{title_ends_with: "1"}}){name, posts{title}}}""", project)
    res.toString should be("""{"data":{"aUsers":[{"name":"Author2","posts":[{"title":"Title1"},{"title":"Title2"}]}]}}""")
  }
}
