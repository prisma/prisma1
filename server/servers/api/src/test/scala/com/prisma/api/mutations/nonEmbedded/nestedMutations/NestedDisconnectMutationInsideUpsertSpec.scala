package com.prisma.api.mutations.nonEmbedded.nestedMutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedDisconnectMutationInsideUpsertSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "a P1 to C1  relation " should "be disconnectable through a nested mutation by id" in {
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
          |    id
          |    childOpt{
          |       id
          |    }
          |  }
          |}""",
          project
        )

      val childId  = res.pathAsString("data.createParent.childOpt.id")
      val parentId = res.pathAsString("data.createParent.id")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      val res2 = server.query(
        s"""
         |mutation {
         |  upsertParent(
         |    where:{id: "$parentId"}
         |    update:{
         |      p: "p2"
         |      childOpt: {disconnect: true}
         |    }
         |    create:{p: "Should not Matter"}
         |  ){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res2.toString should be("""{"data":{"upsertParent":{"childOpt":null}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  "a P1 to C1  relation with the child and the parent without a relation" should "not be disconnectable through a nested mutation by id" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val child1Id = server
        .query(
          """mutation {
          |  createChild(data: {c: "c1"})
          |  {
          |    id
          |  }
          |}""",
          project
        )
        .pathAsString("data.createChild.id")

      val parent1Id = server
        .query(
          """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""",
          project
        )
        .pathAsString("data.createParent.id")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.queryThatMustFail(
        s"""
         |mutation {
         |  upsertParent(
         |  where:{id: "$parent1Id"}
         |  update:{
         |    p: "p2"
         |    childOpt: {disconnect: true}
         |  }
         |  create: {
         |    p:"Should not Matter"
         |  }
         |  ){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project,
        errorCode = 3041
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  "a PM to C1!  relation with the child already in a relation" should "not be disconnectable through a nested mutation by unique" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  upsertParent(
         |    where: {p: "p1"}
         |    update:{
         |    childrenOpt: {disconnect: {c: "c1"}}
         |    }
         |    create: {p: "Should not Matter"}
         |  ){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project,
        errorCode = 3042
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to C1!  relation with the child and the parent already in a relation" should "should error in a nested mutation by unique" in {
    schemaP1optToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  upsertParent(
         |  where: {p: "p1"}
         |  update:{
         |    childOpt: {disconnect: true}
         |  }
         |  create: {p: "Should not Matter"}
         |  ){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project,
        errorCode = 3042,
        errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to C1  relation with the child already in a relation" should "be disconnectable through a nested mutation by unique" in {
    schemaPMToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server
        .query(
          """mutation {
          |  createParent(data: {
          |    p: "p1"
          |    childrenOpt: {
          |      create: [{c: "c1"}, {c: "c2"}]
          |    }
          |  }){
          |    childrenOpt{
          |       c
          |    }
          |  }
          |}""",
          project
        )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      val res = server.query(
        s"""
         |mutation {
         |  upsertParent(
         |  where: { p: "p1"}
         |  update:{
         |    childrenOpt: {disconnect: [{c: "c2"}]}
         |  }
         |  create: {p: "Should not Matter"}
         |  ){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"upsertParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to CM  relation with the child already in a relation" should "be disconnectable through a nested mutation by unique" in {
    schemaP1optToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      val res = server.query(
        s"""
         |mutation {
         |  upsertParent(
         |    where: {p: "p1"}
         |    update:{
         |    childOpt: {disconnect: true}
         |  }
         |    create: {p: "Should not Matter"}
         |  ){
         |    childOpt{
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"upsertParent":{"childOpt":null}}}""")

      server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"c":"c1","parentsOpt":[]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be disconnectable through a nested mutation by unique" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      val res = server.query(
        s"""
         |mutation {
         |  upsertParent(
         |  where: { p: "p1"}
         |  update:{
         |    childrenOpt: {disconnect: [{c: "c1"}]}
         |  }
         |  create: {p: "Should not Matter"}
         |  ){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"upsertParent":{"childrenOpt":[{"c":"c2"}]}}}""")

      server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"children":[{"c":"c1","parentsOpt":[]},{"c":"c2","parentsOpt":[{"p":"p1"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a one to many relation" should "be disconnectable by id through a nested mutation" in {
    val schema = s"""type Comment{
                            id: ID! @id
                            text: String
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @id
                            text: String
                            comments: [Comment] $listInlineDirective
                        }"""

    val project = SchemaDsl.fromStringV11() { schema }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1"}, {text: "comment2"}]
        |      }
        |    }
        |  ){ 
        |    id 
        |    comments { id }
        |  } 
        |}""",
      project
    )

    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")
    val comment2Id = createResult.pathAsString("data.createTodo.comments.[1].id")

    val result = server.query(
      s"""mutation {
         |  upsertTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    update:{
         |      comments: {
         |        disconnect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
         |      }
         |    }
         |    create: {comments: {
         |        create: [{text: "comment3"}, {text: "comment4"}]
         |      }}
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.upsertTodo.comments").toString, """[]""")
  }

  "a one to many relation" should "be disconnectable by any unique argument through a nested mutation" in {
    val schema = s"""type Comment{
                            id: ID! @id
                            text: String
                            alias: String! @unique
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @id
                            text: String
                            comments: [Comment] $listInlineDirective
                        }"""

    val project = SchemaDsl.fromStringV11() { schema }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1", alias: "alias1"}, {text: "comment2", alias: "alias2"}]
        |      }
        |    }
        |  ){
        |    id
        |    comments { id }
        |  }
        |}""",
      project
    )
    val todoId = createResult.pathAsString("data.createTodo.id")

    val result = server.query(
      s"""mutation {
         |  upsertTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    update:{
         |      comments: {
         |        disconnect: [{alias: "alias1"}, {alias: "alias2"}]
         |      }
         |    }
         |    create: {
         |      comments: {
         |        create: [{text: "comment3", alias: "alias4"}, {text: "comment4",alias: "alias5"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.upsertTodo.comments").toString, """[]""")
  }

  "a many to one relation" should "be disconnectable by id through a nested mutation" in {
    val schema = s"""type Comment{
                            id: ID! @id
                            text: String
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @id
                            text: String
                            comments: [Comment] $listInlineDirective
                        }"""

    val project = SchemaDsl.fromStringV11() { schema }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "comment1"}]
        |      }
        |    }
        |  ){
        |    id
        |    comments { id }
        |  }
        |}""",
      project
    )
    val todoId    = createResult.pathAsString("data.createTodo.id")
    val commentId = createResult.pathAsString("data.createTodo.comments.[0].id")

    val result = server.query(
      s"""
         |mutation {
         |  upsertComment(
         |    where: {
         |      id: "$commentId"
         |    }
         |    update: {
         |      todo: {disconnect: true}
         |    }
         |    create: {todo: {
         |        create: {text: "todo4"}
         |      }}
         |
         |  ){
         |    todo {
         |      id
         |    }
         |  }
         |}
      """,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.upsertComment").toString, """{"todo":null}""")
  }

  "a one to one relation" should "be disconnectable by id through a nested mutation" in {
    val schema = """type Note{
                            id: ID! @id
                            text: String
                            todo: Todo @relation(link: INLINE)
                        }

                        type Todo{
                            id: ID! @id
                            title: String!
                            note: Note
                        }"""

    val project = SchemaDsl.fromStringV11() { schema }
    database.setup(project)

    val createResult = server.query(
      """mutation { 
        |  createNote(
        |    data: {
        |      todo: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){ 
        |    id
        |    todo { id }
        |  } 
        |}""",
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")
    val todoId = createResult.pathAsString("data.createNote.todo.id")

    val result = server.query(
      s"""
         |mutation {
         |  upsertNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    update: {
         |      todo: {   disconnect: true}
         |    }
         |    create:{text: "Should not matter", todo: {create:{title: "Also doesnt matter"}}}
         |
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.upsertNote").toString, """{"todo":null}""")
  }

  "a one to many relation" should "be disconnectable by unique through a nested mutation" in {
    val schema = s"""type Comment{
                            id: ID! @id
                            text: String @unique
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @id
                            title: String @unique
                            comments: [Comment] $listInlineDirective
                        }"""

    val project = SchemaDsl.fromStringV11() { schema }
    database.setup(project)

    server.query("""mutation { createTodo(data: {title: "todo"}){ id } }""", project).pathAsString("data.createTodo.id")
    server.query("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    server.query("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      title: "todo"
         |    }
         |    data:{
         |      comments: {
         |        connect: [{text: "comment1"}, {text: "comment2"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[{"text":"comment1"},{"text":"comment2"}]""")

    val result2 = server.query(
      s"""mutation {
         |  upsertTodo(
         |    where: {
         |      title: "todo"
         |    }
         |    update:{
         |      comments: {
         |        disconnect: [{text: "comment2"}]
         |      }
         |    }
         |    create:{
         |      comments: {
         |        create: [{text: "comment3"}, {text: "comment4"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result2.pathAsJsValue("data.upsertTodo.comments").toString, """[{"text":"comment1"}]""")
  }

  "A PM CM self relation" should "be disconnectable by unique through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() { s"""|type User {
                                              |  id: ID! @id
                                              |  banned: Boolean! @default(value: "false")
                                              |  username: String! @unique
                                              |  password: String!
                                              |  salt: String!
                                              |  followers: [User] @relation(name: "UserFollowers" $listInlineArgument)
                                              |  follows: [User] @relation(name: "UserFollows" $listInlineArgument)
                                              |}""" }
    database.setup(project)

    server.query("""mutation { createUser(data: {username: "Paul", password: "1234", salt: "so salty"}){ id } }""", project)
    server.query("""mutation { createUser(data: {username: "Peter", password: "abcd", salt: "so salty"}){ id } }""", project)

    val result = server.query(
      s"""mutation {
         |  updateUser(
         |    where: {
         |      username: "Paul"
         |    }
         |    data:{
         |      follows: {
         |        connect: [{username: "Peter"}]
         |      }
         |    }
         |  ){
         |    username
         |    follows {
         |      username
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateUser.follows").toString, """[{"username":"Peter"}]""")

    val result2 = server.query(
      s"""mutation {
         |  upsertUser(
         |    where: {
         |      username: "Paul"
         |    }
         |    update:{
         |      follows: {
         |        disconnect: [{username: "Peter"}]
         |      }
         |    }
         |    create:{
         |      username: "Paul3",
         |      password: "1234",
         |      salt: "so salty"
         |
         |      follows: {
         |        create: [{username: "Paul2", password: "1234", salt: "so salty"}]
         |      }
         |    }
         |  ){
         |    username
         |    follows {
         |      username
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result2.pathAsJsValue("data.upsertUser.follows").toString, """[]""")
  }

  "A PM CM self relation" should "should throw a correct error for disconnect on invalid unique" in {
    val project = SchemaDsl.fromStringV11() { s"""|type User {
                                              |  id: ID! @id
                                              |  banned: Boolean! @default(value: "false")
                                              |  username: String! @unique
                                              |  password: String!
                                              |  salt: String!
                                              |  followers: [User] @relation(name: "UserFollowers" $listInlineArgument)
                                              |  follows: [User] @relation(name: "UserFollows" $listInlineArgument)
                                              |}""" }
    database.setup(project)

    server.query("""mutation { createUser(data: {username: "Paul", password: "1234", salt: "so salty"}){ id } }""", project)
    server.query("""mutation { createUser(data: {username: "Peter", password: "abcd", salt: "so salty"}){ id } }""", project)
    server.query("""mutation { createUser(data: {username: "Anton", password: "abcd3", salt: "so salty"}){ id } }""", project)

    val result = server.query(
      s"""mutation {
         |  updateUser(
         |    where: {
         |      username: "Paul"
         |    }
         |    data:{
         |      follows: {
         |        connect: [{username: "Peter"}]
         |      }
         |    }
         |  ){
         |    username
         |    follows {
         |      username
         |    }
         |  }
         |}
      """,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateUser.follows").toString, """[{"username":"Peter"}]""")

    server.queryThatMustFail(
      s"""mutation {
         |  upsertUser(
         |    where: {
         |      username: "Paul"
         |    }
         |    update:{
         |      follows: {
         |        disconnect: [{username: "Anton"}]
         |      }
         |    }
         |     create:{
         |      username: "Paul3",
         |      password: "1234",
         |      salt: "so salty"
         |      follows: {
         |        create: [{username: "Paul2", password: "1234", salt: "so salty"}]
         |      }
         |    }
         |  ){
         |    username
         |    follows {
         |      username
         |    }
         |  }
         |}
      """,
      project,
      errorCode = 3041
    )
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path" in {
    val project = SchemaDsl.fromStringV11() { s"""type Top {
                                             |  id: ID! @id
                                             |  nameTop: String! @unique
                                             |  middles: [Middle] $listInlineDirective
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @id
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top]
                                             |  bottoms: [Bottom] $listInlineDirective
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @id
                                             |  nameBottom: String! @unique
                                             |  middles: [Middle]
                                             |}""" }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createTop(data: {
        |    nameTop: "the top",
        |    middles: {
        |      create:[ 
        |        {
        |          nameMiddle: "the middle"
        |          bottoms: {
        |            create: [{ nameBottom: "the bottom"}, { nameBottom: "the second bottom"}]
        |          }
        |        },
        |        {
        |          nameMiddle: "the second middle"
        |          bottoms: {
        |            create: [{nameBottom: "the third bottom"},{nameBottom: "the fourth bottom"}]
        |          }
        |        }
        |     ]
        |    }
        |  }) {id}
        |}
      """

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middles: {
         |        upsert: [{
         |              where: {nameMiddle: "the middle"},
         |              update:{nameMiddle: "updated middle"
         |                      bottoms: {disconnect: [{nameBottom: "the bottom"}]}
         |              },
         |              create:{nameMiddle: "Should not Matter"}
         |       }]
         |     }
         |   }
         |  ) {
         |    nameTop
         |    middles {
         |      nameMiddle
         |      bottoms {
         |        nameBottom
         |      }
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"the second bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path and there are no backrelations" in {
    val project = SchemaDsl.fromStringV11() { s"""type Top {
                                             |  id: ID! @id
                                             |  nameTop: String! @unique
                                             |  middles: [Middle] $listInlineDirective
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @id
                                             |  nameMiddle: String! @unique
                                             |  bottoms: [Bottom] $listInlineDirective
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @id
                                             |  nameBottom: String! @unique
                                             |}""" }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createTop(data: {
        |    nameTop: "the top",
        |    middles: {
        |      create:[
        |        {
        |          nameMiddle: "the middle"
        |          bottoms: {
        |            create: [{ nameBottom: "the bottom"}, { nameBottom: "the second bottom"}]
        |          }
        |        },
        |        {
        |          nameMiddle: "the second middle"
        |          bottoms: {
        |            create: [{nameBottom: "the third bottom"},{nameBottom: "the fourth bottom"}]
        |          }
        |        }
        |     ]
        |    }
        |  }) {id}
        |}
      """

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middles: {
         |        upsert: [{
         |              where: {nameMiddle: "the middle"},
         |              update:{nameMiddle: "updated middle"
         |                      bottoms: {disconnect: [{nameBottom: "the bottom"}]}},
         |              create:{nameMiddle: "Should not matter"}
         |       }]
         |     }
         |   }
         |  ) {
         |    nameTop
         |    middles {
         |      nameMiddle
         |      bottoms {
         |        nameBottom
         |      }
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"the second bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path " in {
    val project = SchemaDsl.fromStringV11() { s"""type Top {
                                             |  id: ID! @id
                                             |  nameTop: String! @unique
                                             |  middles: [Middle] $listInlineDirective
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @id
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top]
                                             |  bottom: Bottom @relation(link: INLINE)
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @id
                                             |  nameBottom: String! @unique
                                             |  middle: Middle
                                             |}""" }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createTop(data: {
        |    nameTop: "the top",
        |    middles: {
        |      create:[
        |        {
        |          nameMiddle: "the middle"
        |          bottom: {create: { nameBottom: "the bottom"}}
        |        },
        |        {
        |          nameMiddle: "the second middle"
        |          bottom: {create: { nameBottom: "the second bottom"}}
        |        }
        |     ]
        |    }
        |  }) {id}
        |}
      """

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middles: {
         |        upsert: [{
         |              where: {nameMiddle: "the middle"},
         |              update:{nameMiddle: "updated middle"
         |                      bottom: {disconnect: true}}
         |              create:{nameMiddle: "Should not matter"}
         |              }]
         |     }
         |   }
         |  ) {
         |    nameTop
         |    middles {
         |      nameMiddle
         |      bottom {
         |        nameBottom
         |      }
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottom":null},{"nameMiddle":"the second middle","bottom":{"nameBottom":"the second bottom"}}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path  and back relations are missing and node edges follow model edges" in {
    val project = SchemaDsl.fromStringV11() { s"""type Top {
                                             |  id: ID! @id
                                             |  nameTop: String! @unique
                                             |  middle: Middle @relation(link: INLINE)
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @id
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom @relation(link: INLINE)
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @id
                                             |  nameBottom: String! @unique
                                             |  below: [Below] $listInlineDirective
                                             |}
                                             |
                                             |type Below {
                                             |  id: ID! @id
                                             |  nameBelow: String! @unique
                                             |}""" }
    database.setup(project)

    val createMutation =
      """
        |mutation a {
        |  createTop(data: {
        |    nameTop: "the top",
        |    middle: {
        |      create:
        |        {
        |          nameMiddle: "the middle"
        |          bottom: {
        |            create: { nameBottom: "the bottom"
        |            below: {
        |            create: [{ nameBelow: "below"}, { nameBelow: "second below"}]}
        |        }}}
        |        }
        |  }) {id}
        |}
      """

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middle: {
         |        update: {
         |               nameMiddle: "updated middle"
         |               bottom: {
         |                upsert: {
         |                  update:{nameBottom: "updated bottom", below: { disconnect: {nameBelow: "below"}}},
         |                  create:{nameBottom: "Should not matter"}
         |              }
         |          }
         |       }
         |     }
         |   }
         |  ) {
         |    nameTop
         |    middle {
         |      nameMiddle
         |      bottom {
         |        nameBottom
         |        below{
         |           nameBelow
         |        }
         |        
         |      }
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom","below":[{"nameBelow":"second below"}]}}}}}""")

    server.query("query{belows{nameBelow}}", project).toString should be("""{"data":{"belows":[{"nameBelow":"below"},{"nameBelow":"second below"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path" in {
    val project = SchemaDsl.fromStringV11() { """type Top {
                                             |  id: ID! @id
                                             |  nameTop: String! @unique
                                             |  middle: Middle @relation(link: INLINE)
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @id
                                             |  nameMiddle: String! @unique
                                             |  top: Top
                                             |  bottom: Bottom @relation(link: INLINE)
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @id
                                             |  middle: Middle
                                             |  nameBottom: String! @unique
                                             |}""" }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createTop(data: {
        |    nameTop: "the top",
        |    middle: {
        |      create: 
        |        {
        |          nameMiddle: "the middle"
        |          bottom: {
        |            create: {
        |              nameBottom: "the bottom"
        |            }
        |          }
        |        }
        |    }
        |  }) {id}
        |}
      """

    server.query(createMutation, project)

    val updateMutation =
      s"""
         |mutation  {
         |  updateTop(
         |    where: {
         |      nameTop: "the top"
         |    }
         |    data: {
         |      nameTop: "updated top",
         |      middle: {
         |        upsert:{
         |          update:{nameMiddle: "updated middle", bottom: {disconnect: true}},
         |          create:{nameMiddle: "Should not Matter"}
         |        }
         |     }
         |   }
         |  ) {
         |    nameTop
         |    middle {
         |      nameMiddle
         |      bottom {
         |        nameBottom
         |      }
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be("""{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":null}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"the bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path and there are no backrelations" in {
    val project = SchemaDsl.fromStringV11() { """type Top {
                                             |  id: ID! @id
                                             |  nameTop: String! @unique
                                             |  middle: Middle @relation(link: INLINE)
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @id
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom @relation(link: INLINE)
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @id
                                             |  nameBottom: String! @unique
                                             |}""" }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createTop(data: {
        |    nameTop: "the top",
        |    middle: {
        |      create:
        |        {
        |          nameMiddle: "the middle"
        |          bottom: {
        |            create: {
        |              nameBottom: "the bottom"
        |            }
        |          }
        |        }
        |    }
        |  }) {id}
        |}
      """

    server.query(createMutation, project)

    val updateMutation =
      s"""
         |mutation  {
         |  updateTop(
         |    where: {
         |      nameTop: "the top"
         |    }
         |    data: {
         |      nameTop: "updated top",
         |      middle: {
         |        upsert: {
         |              update:{nameMiddle: "updated middle",bottom: {disconnect: true}}
         |              create:{nameMiddle: "Should not matter"}
         |      }
         |     }
         |   }
         |  ) {
         |    nameTop
         |    middle {
         |      nameMiddle
         |      bottom {
         |        nameBottom
         |      }
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be("""{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":null}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"the bottom"}]}}""")
  }
}
