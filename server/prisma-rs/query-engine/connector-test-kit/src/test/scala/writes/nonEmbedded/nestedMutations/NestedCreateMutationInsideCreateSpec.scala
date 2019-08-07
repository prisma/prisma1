package writes.nonEmbedded.nestedMutations

import java.util.UUID

import org.scalatest.{Matchers, WordSpecLike}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NestedCreateMutationInsideCreateSpec extends WordSpecLike with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "a P1! to C1! relation should be possible" in {
    schemaP1reqToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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
            |}""".stripMargin,
          project
        )

      res.toString should be("""{"data":{"createParent":{"p":"p1","childReq":{"c":"c1"}}}}""")

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1! to C1 relation should work" in {
    schemaP1reqToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val child1Id = server
        .query(
          """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childReq: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
          project
        )
        .pathAsString("data.createParent.childReq.id")

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to C1 relation should work" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to C1! should work" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a P1 to C1! relation  should work" in {
    schemaP1optToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to C1 relation should work" in {
    schemaPMToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val res = server
        .query(
          """mutation {
            |  createParent(data: {
            |    p: "p1"
            |    childrenOpt: {
            |      create: [{c: "c1"},{c: "c2"}]
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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a P1! to CM  relation  should work" in {
    schemaP1reqToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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
            |   childReq{
            |     c
            |   }
            |  }
            |}""".stripMargin,
          project
        )

      res.toString should be("""{"data":{"createParent":{"childReq":{"c":"c1"}}}}""")

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to CM relation should work" in {
    schemaP1optToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      // make sure it is traversable in the opposite direction as well
      val queryResult = server.query(
        """
          |{
          |  children {
          |    parentsOpt {
          |      p
          |    }
          |  }
          |}
        """.stripMargin,
        project
      )

      queryResult.toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p1"}]}]}}""")

    }
  }

  "a PM to CM  relation  should work" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a one to many relation should be creatable through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""model Todo{
        |   id        String    @id @default(cuid())
        |   comments  Comment[] $listInlineDirective
        |}
        |
        |model Comment{
        |   id    String @id @default(cuid())
        |   text  String
        |   todo  Todo?
        |}"""
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

  "a many to one relation should be creatable through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      """model Todo{
        |   id       String   @id @default(cuid())
        |   title    String
        |   comments Comment? @relation(references: [id])
        |}
        |
        |model Comment{
        |   id   String @id @default(cuid())
        |   text String
        |   todo Todo?
        |}"""
    }

    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  createComment(data: {
        |    text: "comment1"
        |    todo: {
        |      create: {title: "todo1"}
        |    }
        |  }){
        |    id
        |    todo {
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.createComment.todo.title"), "todo1")
  }

  "a many to many relation should be creatable through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""model Todo{
        |   id     String @id @default(cuid())
        |   title  String
        |   tags   Tag[] $listInlineDirective
        |}
        |
        |model Tag{
        |   id    String @id @default(cuid())
        |   name  String
        |   todos Todo[]
        |}"""
    }

    database.setup(project)

    val result = server
      .query(
        """
        |mutation {
        |  createTodo(data:{
        |    title: "todo1"
        |    tags: {
        |      create: [{name: "tag1"}, {name: "tag2"}]
        |    }
        |  }){
        |    id
        |    tags {
        |      name
        |    }
        |  }
        |}
      """.stripMargin,
        project
      )

    mustBeEqual(result.pathAsJsValue("data.createTodo.tags").toString, """[{"name":"tag1"},{"name":"tag2"}]""")

    val result2 = server.query(
      """
        |mutation {
        |  createTag(data:{
        |    name: "tag1"
        |    todos: {
        |      create: [{title: "todo1"}, {title: "todo2"}]
        |    }
        |  }){
        |    id
        |    todos {
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result2.pathAsJsValue("data.createTag.todos").toString, """[{"title":"todo1"},{"title":"todo2"}]""")
  }

  "A nested create on a one to one relation should correctly assign violations to offending model and not partially execute first direction" in {
    val project = SchemaDsl.fromStringV11() {
      """model User{
        |   id     String  @id @default(cuid())
        |   name   String
        |   unique String? @unique
        |   post   Post?   @relation(references: [id])
        |}
        |
        |model Post{
        |   id         String @id @default(cuid())
        |   title      String
        |   uniquePost String? @unique
        |   user       User?
        |}"""
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
  }

  "A nested create on a one to one relation should correctly assign violations to offending model and not partially execute second direction" in {
    val project = SchemaDsl.fromStringV11() {
      """model User{
        |   id      String  @id @default(cuid())
        |   name    String
        |   unique  String? @unique
        |   post    Post?   @relation(references: [id])
        |}
        |
        |model Post{
        |   id         String @id @default(cuid())
        |   title      String
        |   uniquePost String? @unique
        |   user       User?
        |}"""
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

    ifConnectorIsNotMongo(server.query("query{users{id}}", project).pathAsSeq("data.users").length should be(1))
    server.query("query{posts{id}}", project).pathAsSeq("data.posts").length should be(1)
  }

  "a deeply nested mutation should execute all levels of the mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""model List{
        |   id    String @id @default(cuid())
        |   name  String
        |   todos Todo[] $listInlineDirective
        |}
        |
        |model Todo{
        |   id     String @id @default(cuid())
        |   title  String
        |   list   List?
        |   tag    Tag?   @relation(references: [id])
        |}
        |
        |model Tag{
        |   id    String @id @default(cuid())
        |   name  String
        |   todo  Todo?
        |}"""
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

  "a required one2one relation should be creatable through a nested create mutation" in {

    val project = SchemaDsl.fromStringV11() {
      """model Comment{
        |   id           String  @id @default(cuid())
        |   reqOnComment String
        |   optOnComment String?
        |   todo         Todo    @relation(references: [id])
        |}
        |
        |model Todo{
        |   id        String @id @default(cuid())
        |   reqOnTodo String
        |   optOnTodo String?
        |   comment   Comment
        |}"""
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
      errorContains = "The field 'todo' on model 'Comment' is required. Performing this mutation would violate that constraint"
    )
  }

  "a required one2one relation should be creatable through a nested connected mutation" in {

    val project = SchemaDsl.fromStringV11() {
      """model Comment{
        |   id           String @id @default(cuid())
        |   reqOnComment String
        |   optOnComment String?
        |   todo         Todo    @relation(references: [id])
        |}
        |
        |model Todo{
        |   id        String @id @default(cuid())
        |   reqOnTodo String
        |   optOnTodo String?
        |   comment   Comment?
        |}"""
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
        |    todo{
        |       reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsString("data.createComment.todo.reqOnTodo"), "todo1")

    server.query("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(1)
    server.query("{ comments { id } }", project).pathAsSeq("data.comments").size should be(1)

    // TODO: originally the argument had empty arguments for todo: {} in there. Our schema could not handle that yet.
    server.queryThatMustFail(
      """
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment2"
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
      errorContains = "The field 'todo' on model 'Comment' is required. Performing this mutation would violate that constraint"
    )

    server.query("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(1)
    server.query("{ comments { id } }", project).pathAsSeq("data.comments").size should be(1)

    val todoId = server
      .query(
        """
        |mutation {
        |  createTodo(data: {
        |       reqOnTodo: "todo2"
        |  })
        |  {
        |    id
        |  }
        |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    server.query("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(2)
    server.query("{ comments { id } }", project).pathAsSeq("data.comments").size should be(1)

    server.query(
      s"""
        |mutation {
        |  createComment(data: {
        |    reqOnComment: "comment3"
        |    todo: {
        |      connect: {id: "$todoId"}
        |    }
        |  }){
        |    id
        |    todo{
        |       reqOnTodo
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    server.query("{ todoes { id } }", project).pathAsSeq("data.todoes").size should be(2)
    server.query("{ comments { id } }", project).pathAsSeq("data.comments").size should be(2)

  }

  "creating a nested item with an id of model UUID should work" taggedAs (IgnoreMySql, IgnoreMongo, IgnoreSQLite) in {
    val project = SchemaDsl.fromStringV11() {
      s"""
         |model List {
         |  id     String @id @default(cuid())
         |  todos  Todo[]
         |}
         |
         |model Todo {
         |  id    String @id @default(uuid())
         |  title String
         |}
       """.stripMargin
    }
    database.setup(project)

    val result = server.query(
      """
        |mutation {
        |  createList(data: {
        |    todos: {
        |      create: [ {title: "the todo"} ]
        |    }
        |  }){
        |    todos {
        |      id
        |      title
        |    }
        |  }
        |}
      """.stripMargin,
      project
    )

    result.pathAsString("data.createList.todos.[0].title") should equal("the todo")
    val theUuid = result.pathAsString("data.createList.todos.[0].id")
    UUID.fromString(theUuid) // should now blow up
  }

  "Backrelation bug should be fixed" in {

    val project = SchemaDsl.fromStringV11() {
      s"""
        |model User {
        |  id          String           @id @default(cuid())
        |  nick        String           @unique
        |  memberships ListMembership[]
        |}
        |
        |model List {
        |  id          String   @id @default(cuid())
        |  createdAt   DateTime @default(now())
        |  updatedAt   DateTime @updatedAt
        |  name        String
        |  memberships ListMembership[]
        |}
        |
        |model ListMembership {
        |  id   String @id @default(cuid())
        |  user User   @relation(references: [id])
        |  list List   @relation(references: [id])
        |}"""
    }

    database.setup(project)

    val create = server.query(
      s"""mutation createUser {
                  createUser(data: {
                    nick: "marcus"
                    memberships: {
                      create: [
                        {
                          list: {
                            create: {
                              name: "Personal Inbox"
                            }
                          }
                        }
                      ]
                    }
                  }){
                    nick
                  }
                }""",
      project
    )

    create.toString should be("""{"data":{"createUser":{"nick":"marcus"}}}""")

    val result = server.query(
      s"""query users {
                  users{
                    nick
                    memberships {
                      list {
                        name
                      }
                    }
                  }
                }""",
      project
    )

    result.toString should be("""{"data":{"users":[{"nick":"marcus","memberships":[{"list":{"name":"Personal Inbox"}}]}]}}""")
  }
}
