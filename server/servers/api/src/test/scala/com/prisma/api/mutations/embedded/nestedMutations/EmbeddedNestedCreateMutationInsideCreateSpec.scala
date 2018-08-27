package com.prisma.api.mutations.embedded.nestedMutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedNestedCreateMutationInsideCreateSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def onlyRunSuiteForMongo: Boolean = true

  "a P1! relation" should "be possible" in {

    val project = SchemaDsl.fromString() {
      """
         |type Parent{
         | id: ID! @unique
         | p: String! @unique
         | childReq: Child!
         |}
         |
         |type Child @embedded {
         | c: String! @unique
         |}
       """
    }

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
    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        | id: ID! @unique
        | p: String! @unique
        | childOpt: Child
        |}
        |
        |type Child @embedded {
        | c: String! @unique
        |}
      """
    }
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
    val project = SchemaDsl.fromString() {
      """
        |type Parent{
        | id: ID! @unique
        | p: String! @unique
        | childrenOpt: [Child!]!
        |}
        |
        |type Child @embedded {
        | c: String! @unique
        |}
      """
    }
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
        | comments: [Comment!]!
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
        | todos: [Todo!]!
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
}
