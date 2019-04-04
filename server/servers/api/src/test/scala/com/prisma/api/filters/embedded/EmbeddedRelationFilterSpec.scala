package com.prisma.api.filters.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._

class EmbeddedRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  lazy val project = SchemaDsl.fromStringV11() {
    """
      |type Blog {
      |   id: ID! @id
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

  "1 level m-relation filter" should "work for _some" in {

    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 5}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{popularity_gte: 50}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

  "1 level m-relation filter" should "work for _every" in {

    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 2}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"},{"name":"blog 2"}]}}""")

    server.query(query = """{blogs(where:{posts_every:{popularity_gte: 3}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

  "1 level m-relation filter" should "work for _none" in {

    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 50}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_none:{popularity_gte: 5}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
  }

  "2 level m-relation filter" should "work for _some" in {

    // some|some
    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 0}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")

    server.query(query = """{blogs(where:{posts_some:{comments_some: {likes: 1}}}){name}}""", project = project).toString should be("""{"data":{"blogs":[]}}""")
  }

  "2 level m-relation filter" should "work for _every, _some and _none" in {

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

  "2 level m- and 1-relation filter" should "work for _some" in {

    // some|one
    server.query(query = """{blogs(where:{posts_some:{author: {name: "Author1"}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 1"}]}}""")
  }

  "2 level m- and 1-relation filter" should "work for _every" in {

    // every|one
    server.query(query = """{blogs(where:{posts_every:{author: {name_ends_with: "3"}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

  "2 level m- and 1-relation filter" should "work for _none" in {

    // none|one
    server.query(query = """{blogs(where:{posts_none:{author: {name: "Author2"}}}){name}}""", project = project).toString should be(
      """{"data":{"blogs":[{"name":"blog 2"}]}}""")
  }

  "Fancy filter" should "work" in {

    val project = SchemaDsl.fromStringV11() {
      """
        |type User {
        |  id: ID! @id
        |  name: String!
        |  pets: [Dog]
        |  posts: [Post]
        |}
        |
        |type Post {
        |  id: ID! @id
        |  author: User @relation(link: INLINE)
        |  title: String!
        |  createdAt: DateTime! @createdAt
        |  updatedAt: DateTime! @updatedAt
        |}
        |
        |type Walker {
        |  id: ID! @id
        |  name: String!
        |}
        |
        |type Dog @embedded {
        |  breed: String!
        |  walker: Walker @relation(link: INLINE) @db(name: "dogtowalker")
        |}"""
    }

    database.setup(project)

    val query = """mutation create {
                  |  createUser(
                  |    data: {
                  |      name: "User"
                  |      pets: {
                  |        create: [
                  |          { breed: "Breed 1", walker: { create: { name: "Walker 1" } } }
                  |          { breed: "Breed 1", walker: { create: { name: "Walker 1" } } }
                  |        ]
                  |      }
                  |    }
                  |  ) {
                  |    name
                  |    pets {
                  |      breed
                  |      walker {
                  |        name
                  |      }
                  |    }
                  |  }
                  |}"""

    server.query(query, project).toString should be(
      """{"data":{"createUser":{"name":"User","pets":[{"breed":"Breed 1","walker":{"name":"Walker 1"}},{"breed":"Breed 1","walker":{"name":"Walker 1"}}]}}}""")

    val query2 = """query withFilter {
                   |  users(
                   |    where: {
                   |      name: "User"
                   |      pets_some: { breed: "Breed 1", walker: { name: "Walker 2" } }
                   |    }
                   |  ) {
                   |    name
                   |    pets {
                   |      breed
                   |      walker {
                   |        name
                   |      }
                   |    }
                   |  }
                   |}"""

    server.query(query2, project).toString should be("""{"data":{"users":[]}}""")

    val query3 = """query withFilter {
                   |  users(
                   |    where: {
                   |      name: "User"
                   |      pets_some: { breed: "Breed 1", walker: { name: "Walker 1" } }
                   |    }
                   |  ) {
                   |    name
                   |    pets {
                   |      breed
                   |      walker {
                   |        name
                   |      }
                   |    }
                   |  }
                   |}"""

    server.query(query3, project).toString should be(
      """{"data":{"users":[{"name":"User","pets":[{"breed":"Breed 1","walker":{"name":"Walker 1"}},{"breed":"Breed 1","walker":{"name":"Walker 1"}}]}]}}""")
  }

  "Self relations bug" should "be fixed" in {

    val project = SchemaDsl.fromStringV11() {
      """
        |type User {
        |  id: ID! @id
        |  updatedAt: DateTime! @updatedAt
        |  nick: String! @unique
        |}
        |
        |type Todo {
        |  id: ID! @id
        |  title: String! @unique
        |  comments: [Comment]
        |}
        |
        |type Comment @embedded {
        |  text: String!
        |  user: User! @relation(link: INLINE)
        |  snarkyRemark: Comment
        |}"""
    }

    database.setup(project)

    val create = server.query("""mutation{createTodo(data:{title:"todoTitle"}){title}}""", project)
    create.toString should be("""{"data":{"createTodo":{"title":"todoTitle"}}}""")

    val create2 = server.query("""mutation{createUser(data:{nick:"Marcus"}){nick}}""", project)
    create2.toString should be("""{"data":{"createUser":{"nick":"Marcus"}}}""")

    val update = server.query(
      s"""mutation c{
  updateTodo(
    where: { title: "todoTitle" }
    data: {
      comments: {
        create: [
          {
            text:"This is very important"
            user: {
              connect: {
                nick: "Marcus"
              }
            }
            snarkyRemark: {
              create: {
                text:"This is very very imporanto!"
                user: {
                  connect: {nick:"Marcus"}
                }
              }
            }
          }
        ]
      }
    }
  ){
    title
  }
}""",
      project
    )

    update.toString should be("""{"data":{"updateTodo":{"title":"todoTitle"}}}""")

    val result = server.query(
      s"""query commentsOfAUser {
         |  todoes(where: {
         |    comments_some: {
         |      text_contains: "This"
         |    }
         |  }) {
         |    title
         |    comments {
         |      text
         |      snarkyRemark{
         |         text
         |         user{
         |            nick
         |         }
         |      }
         |    }
         |  }
         |} """,
      project
    )

    result.toString should be(
      """{"data":{"todoes":[{"title":"todoTitle","comments":[{"text":"This is very important","snarkyRemark":{"text":"This is very very imporanto!","user":{"nick":"Marcus"}}}]}]}}""")
  }

}
