package com.prisma.api.filters

import com.prisma.IgnorePostgres
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class RelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

//  override def runSuiteOnlyForActiveConnectors = true

  val project = SchemaDsl.fromBuilder { schema =>
    val blog = schema
      .model("Blog")
      .field_!("name", _.String)
    val post = schema
      .model("Post")
      .field_!("title", _.String)
      .field_!("popularity", _.Int)
      .manyToOneRelation("blog", "posts", blog)
    val comment = schema
      .model("Comment")
      .field_!("likes", _.Int)
      .field_!("text", _.String)
      .manyToOneRelation("post", "comments", post)
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

  "1 level m-relation filter" should "work for _every, _some and _none" in {

    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 5}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 50}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 2}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 3}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 50}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 5}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
  }

  "2 level m-relation filter" should "work for _every, _some and _none" in {

    // some|some
    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 1}}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")

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

  "crazy filters" should "work" in {

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

    /**
      * select * from Post
      * inner join Blog on Blog.post_id = Post.id
      * inner join Post as Post_Sub on Blog.post_id = Post_Sub.id
      * where Post_Sub.popularity > 5
      */
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
                |    likes_gte: 500
                |  }
                |  comments_some: {
                |    likes_lte: 2
                |  }
                |}) {
                |  title
                |}}""".stripMargin,
        project = project
      )
      .toString should be("""{"data":{"posts":[{"title":"post 1"}]}}""")
  }
}
