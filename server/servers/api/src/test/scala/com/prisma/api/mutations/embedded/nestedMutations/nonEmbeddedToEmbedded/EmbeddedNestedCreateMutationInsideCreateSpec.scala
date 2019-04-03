package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.api.mutations.nonEmbedded.nestedMutations.SchemaBaseV11
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedNestedCreateMutationInsideCreateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "a P1! relation" should "be possible" in {
    val project = SchemaDsl.fromStringV11() { embeddedP1req }
    database.setup(project)

    val res = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    p
          |    childReq{
          |       c
          |    }
          |  }
          |}""",
        project
      )

    res.toString should be("""{"data":{"createParent":{"p":"p1","childReq":{"c":"c1"}}}}""")
  }

  "a P1 relation" should "work" in {
    val project = SchemaDsl.fromStringV11() { embeddedP1opt }
    database.setup(project)

    val res = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childOpt: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |   childOpt{
          |     c
          |   }
          |  }
          |}""".stripMargin,
        project
      )

    res.toString should be("""{"data":{"createParent":{"childOpt":{"c":"c1"}}}}""")
  }

  "a PM relation" should "work" in {
    val project = SchemaDsl.fromStringV11() { embeddedPM }
    database.setup(project)

    val res = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childrenOpt: {
          |      create: [{c: "c1"},{c:"c2"}]
          |    }
          |  }){
          |   childrenOpt{
          |     c
          |   }
          |  }
          |}""".stripMargin,
        project
      )

    res.toString should be("""{"data":{"createParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(2) }
  }

  "a one to many relation" should "be creatable through a nested mutation" in {
    val project = SchemaDsl.fromString() {
      """
        |type Todo{
        | id: ID! @unique
        | comments: [Comment]
        |}
        |
        |type Comment @embedded {
        | text: String!
        |}
      """
    }
    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  createTodo(data:{
        |    comments: {
        |      create: [{text: "comment1"}, {text: "comment2"}]
        |    }
        |  }){
        |    id
        |    comments {
        |      text
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.createTodo.comments").toString, """[{"text":"comment1"},{"text":"comment2"}]""")
  }

  "A nested create on a one to one relation" should "correctly assign violations to offending model and not partially execute" ignore {
    val project = SchemaDsl.fromBuilder { schema =>
      val user = schema.model("User").field_!("name", _.String).field("unique", _.String, isUnique = true)
      schema.model("Post").field_!("title", _.String).field("uniquePost", _.String, isUnique = true).oneToOneRelation("user", "post", user)
    }
    database.setup(project)

    server.query(
      """mutation{
        |  createUser(data:{
        |    name: "Paul"
        |    unique: "uniqueUser"
        |    post: {create:{title: "test"    uniquePost: "uniquePost"}
        |    }
        |  })
        |    {id}
        |  }
      """.stripMargin,
      project
    )

    server.query("query{users{id}}", project).pathAsSeq("data.users").length should be(1)
    server.query("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)

    server.queryThatMustFail(
      """mutation{
        |  createUser(data:{
        |    name: "Paul2"
        |    unique: "uniqueUser"
        |    post: {create:{title: "test2"    uniquePost: "uniquePost2"}
        |    }
        |  })
        |    {id}
        |  }
      """.stripMargin,
      project,
      errorCode = 3010,
      errorContains = "A unique constraint would be violated on User. Details: Field name = unique"
    )

    server.query("query{users{id}}", project).pathAsSeq("data.users").length should be(1)
    server.query("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)

    server.queryThatMustFail(
      """mutation{
        |  createUser(data:{
        |    name: "Paul2"
        |    unique: "uniqueUser2"
        |    post: {create:{title: "test2"    uniquePost: "uniquePost"}
        |    }
        |  })
        |    {id}
        |  }
      """.stripMargin,
      project,
      errorCode = 3010,
      errorContains = "A unique constraint would be violated on Post. Details: Field name = uniquePost"
    )

    server.query("query{users{id}}", project).pathAsSeq("data.users").length should be(1)
    server.query("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)
  }

  "a deeply nested mutation" should "execute all levels of the mutation" in {
    val project = SchemaDsl.fromString() {
      """
        |type List{
        | id: ID! @unique
        | name: String!
        | todos: [Todo]
        |}
        |
        |type Todo @embedded {
        | title: String!
        | tag: Tag
        |}
        |
        |type Tag @embedded {
        | name: String!
        |}
      """
    }

    database.setup(project)

    val mutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tag: {
        |            create: {
        |              name: "the tag"
        |            }
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    name
        |    todos {
        |      title
        |      tag {
        |        name
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val result = server.query(mutation, project)
    result.pathAsString("data.createList.name") should equal("the list")
    result.pathAsString("data.createList.todos.[0].title") should equal("the todo")
    result.pathAsString("data.createList.todos.[0].tag.name") should equal("the tag")
  }

  "a required one2one relation" should "be creatable through a nested create mutation" in {

    val project = SchemaDsl.fromString() {
      """
        |type Comment{
        | id: ID! @unique
        | reqOnComment: String!
        | todo: Todo!
        |}
        |
        |type Todo @embedded {
        | reqOnTodo: String!
        |}
      """
    }

    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {
        |      create: {reqOnTodo: "todo1"}
        |    }
        |  }){
        |    id
        |    todo{reqOnTodo}
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.createComment.todo.reqOnTodo"), "todo1")

    server.queryThatMustFail(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment1"
        |    todo: {}
        |  }){
        |    id
        |    todo {
        |      reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project,
      errorCode = 3032,
      errorContains = "The field 'todo' on type 'Comment' is required. Performing this mutation would violate that constraint"
    )
  }

  "Deeply nested create" should "work" in {

    val project = SchemaDsl.fromString() {
      """
        |type User {
        |  id: ID! @unique
        |  name: String!
        |  pets: [Dog]
        |  posts: [Post]
        |}
        |
        |type Post {
        |  id: ID! @unique
        |  author: User @mongoRelation(field: "author")
        |  title: String!
        |  createdAt: DateTime!
        |  updatedAt: DateTime!
        |}
        |
        |type Walker {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type Dog @embedded {
        |  breed: String!
        |  walker: Walker @mongoRelation(field: "dogtowalker")
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
  }

  "To one relations" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |   createdAt: DateTime!
        |}
        |
        |type Middle @embedded{
        |   unique: Int! @unique
        |   name: String!
        |   bottom: Bottom
        |   createdAt: DateTime!
        |}
        |
        |type Bottom @embedded{
        |   unique: Int! @unique
        |   name: String!
        |   updatedAt: DateTime!
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |   middle: {create:{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:{
         |          unique: 111,
         |          name: "Bottom"
         |      }}
         |   }}
         |}){
         |  unique,
         |  middle{
         |    unique,
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11,"bottom":{"unique":111}}}}}""")
  }

  "To many relations" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: [Middle]
        |}
        |
        |type Middle @embedded {
        |   unique: Int! @unique
        |   name: String!
        |   bottom: [Bottom]
        |}
        |
        |type Bottom @embedded{
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |   middle: {create:[{
         |      unique: 11,
         |      name: "Middle"
         |      bottom: {create:{
         |          unique: 111,
         |          name: "Bottom"
         |      }}},
         |      {
         |      unique: 12,
         |      name: "Middle2"
         |      bottom: {create:{
         |          unique: 112,
         |          name: "Bottom2"
         |      }}
         |    }]
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique,
         |    bottom{
         |      unique
         |    }
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":[{"unique":11,"bottom":[{"unique":111}]},{"unique":12,"bottom":[{"unique":112}]}]}}}""")
  }
  "Relations from embedded to Non-Embedded" should "work 1" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String
        |    child: Child
        |}
        |
        |type Friend{
        |    id: ID! @unique
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friend: Friend @mongoRelation(field: "friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   child: {create:{
         |      name: "Daughter"
         |      friend: {create:{
         |          name: "Buddy"
         |      }
         |      }
         |   }}
         |}){
         |  name,
         |  child{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","child":{"name":"Daughter","friend":{"name":"Buddy"}}}}}""")
  }

  "Relations from embedded to Non-Embedded" should "work 2" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String @unique
        |    children: [Child]
        |}
        |
        |type Friend{
        |    id: ID! @unique
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String @unique
        |    friend: Friend @mongoRelation(field: "friend")
        |}"""
    }

    database.setup(project)

    server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:{
         |      name: "Daughter"
         |   }}
         |}){
         |  name,
         |  children{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    val res = server.query(
      s"""mutation {
         |   updateParent(
         |   where:{name: "Dad"}
         |   data: {
         |   children: {update:{
         |      where: {name: "Daughter"}
         |      data: {
         |          friend:{create:{name: "Buddy"}}
         |      }
         |   }}
         |}){
         |  name,
         |  children{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"updateParent":{"name":"Dad","children":[{"name":"Daughter","friend":{"name":"Buddy"}}]}}}""")
  }

  "Relations from embedded to Non-Embedded" should "work 3" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String
        |    children: [Child]
        |}
        |
        |type Friend{
        |    id: ID! @unique
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friend: Friend @mongoRelation(field: "friend")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:{
         |      name: "Daughter"
         |      friend: {create:{
         |          name: "Buddy"
         |      }
         |      }
         |   }}
         |}){
         |  name,
         |  children{
         |    name
         |    friend{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be("""{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter","friend":{"name":"Buddy"}}]}}}""")
  }

  "Relations from embedded to Non-Embedded" should "work 4" in {

    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        |    id: ID! @unique
        |    name: String
        |    children: [Child]
        |}
        |
        |type Friend{
        |    id: ID! @unique
        |    name: String
        |}
        |
        |type Child @embedded {
        |    name: String
        |    friends: [Friend] @mongoRelation(field: "friends")
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createParent(data: {
         |   name: "Dad",
         |   children: {create:[
         |   {name: "Daughter", friends: {create:[{name: "Buddy"},{name: "Buddy2"}]}},
         |   {name: "Daughter2", friends: {create:[{name: "Buddy3"},{name: "Buddy4"}]}}
         |   ]}
         |}){
         |  name,
         |  children{
         |    name
         |    friends{
         |      name
         |    }
         |  }
         |}}""",
      project
    )

    res.toString should be(
      """{"data":{"createParent":{"name":"Dad","children":[{"name":"Daughter","friends":[{"name":"Buddy"},{"name":"Buddy2"}]},{"name":"Daughter2","friends":[{"name":"Buddy3"},{"name":"Buddy4"}]}]}}}""")
  }

  "Triple nested create" should "work " in {

    val project = SchemaDsl.fromString() {
      """
        |type MealPlan {
        |   id: ID! @unique
        |   menuItem: MenuItem @relation(name: "TEST", link: INLINE)
        |   subtotal: Int
        |}
        |
        |type MenuItem {
        |   id: ID! @unique
        |   name: String
        |   image: [menuImage]
        |}
        |
        |type menuImage @embedded {
        |   name: String
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
  createMealPlan(
    data: {
      subtotal: 123
      menuItem: {
        create: {
          name: "asd"
          image: { create: [{ name: "adsw"}] }
        }
      }
    }
  ) {
    menuItem{
      image{
        name
      }
    }
  }
}""",
      project
    )

    res.toString should be("""{"data":{"createMealPlan":{"menuItem":{"image":[{"name":"adsw"}]}}}}""")
  }

}
