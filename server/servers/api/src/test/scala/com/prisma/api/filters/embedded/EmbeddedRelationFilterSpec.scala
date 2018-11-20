package com.prisma.api.filters.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class EmbeddedRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  lazy val project = SchemaDsl.fromString() {
    """
      |type Blog {
      |   id: ID! @unique
      |   name: String!
      |   posts: [Post]
      |}
      |
      |type Post @embedded {
      |   title: String!
      |   popularity: Int!
      |   comments: [Comment]
      |   author: Author
      |}
      |
      |type Comment @embedded{
      |   text: String!
      |   likes: Int!
      |}
      |
      |type Author @embedded{
      |   name: String!
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
        |           { title: "post 1",
        |             popularity: 10,
        |             comments:{
        |                        create: [{text:"comment 1", likes: 0 },
        |                                 {text:"comment 2", likes: 5},
        |                                 {text:"comment 3", likes: 10}]
        |             },
        |             author: {create:{name: "Author1"}}
        |           },
        |           { title: "post 2",
        |             popularity: 2,
        |             comments:{
        |                        create: [{text:"comment 4", likes: 10}]
        |             },
        |             author: {create:{name: "Author2"}}
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
        |                                             ]},
        |                                    author: {create:{name: "Author3"}}
        |                                             }]}
        |                                             }){name}}""".stripMargin,
      project = project
    )
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

  "2 level m- and 1-relation filter" should "work for _every, _some and _none" in {

    // some|one
    server.query(query = """{blogs(where:{posts_some:{author: {name: "Author1"}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    // every|one
    server.query(query = """{blogs(where:{posts_every:{author: {name_ends_with: "3"}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")

    // none|one
    server.query(query = """{blogs(where:{posts_none:{author: {name: "Author2"}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

}
