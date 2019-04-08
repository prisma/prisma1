package com.prisma.api.mutations.nonEmbedded.nestedMutations

import com.prisma.{IgnoreMongo, IgnoreSQLite}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedDisconnectMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "a P1 to C1 relation " should "be disconnectable through a nested mutation by id" in {
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
          |}""".stripMargin,
          project
        )

      val childId  = res.pathAsString("data.createParent.childOpt.id")
      val parentId = res.pathAsString("data.createParent.id")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      val res2 = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childOpt: {disconnect: true}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res2.toString should be("""{"data":{"updateParent":{"childOpt":null}}}""")

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
          |}""".stripMargin,
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
          |}""".stripMargin,
          project
        )
        .pathAsString("data.createParent.id")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {disconnect: true}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
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
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {disconnect: {c: "c1"}}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
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
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childOpt: {disconnect: true}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project,
        errorCode = 3042,
        errorContains = "The change you are trying to make would violate the required relation 'ChildToParent' between Child and Parent"
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to C1 relation with the child already in a relation" should "be disconnectable through a nested mutation by unique" in {
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
          |}""".stripMargin,
          project
        )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {disconnect: [{c: "c2"}]}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"}]}}}""")

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
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childOpt: {disconnect: true}
         |  }){
         |    childOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childOpt":null}}}""")

      server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"c":"c1","parentsOpt":[]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be disconnectable through a nested mutation by unique" taggedAs (IgnoreMongo, IgnoreSQLite) in { // TODO: Remove ignore when enabling transactions
    // since this assumes transactionality, test ist split below
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
        |}""".stripMargin,
        project
      )

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "otherParent"
        |    childrenOpt: {
        |      create: [{c: "otherChild"}]
        |      connect: [{c: "c1"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {disconnect: [{c: "c1"}, {c: "otherChild"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project,
        3041
      )

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {disconnect: [{c: "c1"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c2"}]}}}""")

      server.query(s"""query{child(where:{c:"c1"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"c1","parentsOpt":[{"p":"otherParent"}]}}}""")

      server.query(s"""query{child(where:{c:"c2"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"c2","parentsOpt":[{"p":"p1"}]}}}""")

      server.query(s"""query{child(where:{c:"otherChild"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"otherChild","parentsOpt":[{"p":"otherParent"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(3) }
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be disconnectable through a nested mutation by unique 2" in {
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
        |}""".stripMargin,
        project
      )

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "otherParent"
        |    childrenOpt: {
        |      create: [{c: "otherChild"}]
        |      connect: [{c: "c1"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {disconnect: [{c: "c1"}, {c: "otherChild"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project,
        3041
      )
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be disconnectable through a nested mutation by unique 3" in {
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
        |}""".stripMargin,
        project
      )

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "otherParent"
        |    childrenOpt: {
        |      create: [{c: "otherChild"}]
        |      connect: [{c: "c1"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {disconnect: [{c: "c1"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c2"}]}}}""")

      server.query(s"""query{child(where:{c:"c1"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"c1","parentsOpt":[{"p":"otherParent"}]}}}""")

      server.query(s"""query{child(where:{c:"c2"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"c2","parentsOpt":[{"p":"p1"}]}}}""")

      server.query(s"""query{child(where:{c:"otherChild"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"otherChild","parentsOpt":[{"p":"otherParent"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(3) }
    }
  }

  "a one to many relation" should "be disconnectable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo{
            id: ID! @id
            comments: [Comment] $listInlineDirective
        }

        type Comment{
            id: ID! @id
            text: String
            todo: Todo
        }"""
    }

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
        |}""".stripMargin,
      project
    )

    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")
    val comment2Id = createResult.pathAsString("data.createTodo.comments.[1].id")

    val result = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        disconnect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[]""")
  }

  "a one to many relation" should "be disconnectable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo{
              id: ID! @id
              comments: [Comment] $listInlineDirective
          }

          type Comment{
              id: ID! @id
              text: String
              alias: String! @unique
              todo: Todo
          }"""
    }

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
        |}""".stripMargin,
      project
    )
    val todoId = createResult.pathAsString("data.createTodo.id")

    val result = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        disconnect: [{alias: "alias1"}, {alias: "alias2"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[]""")
  }

  "a many to one relation" should "be disconnectable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo{
              id: ID! @id
              comments: [Comment] $listInlineDirective
          }

          type Comment{
              id: ID! @id
              text: String
              todo: Todo
          }"""
    }

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
        |}""".stripMargin,
      project
    )
    val todoId    = createResult.pathAsString("data.createTodo.id")
    val commentId = createResult.pathAsString("data.createTodo.comments.[0].id")

    val result = server.query(
      s"""
         |mutation {
         |  updateComment(
         |    where: {
         |      id: "$commentId"
         |    }
         |    data: {
         |      todo: {disconnect: true}
         |    }
         |  ){
         |    todo {
         |      id
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.updateComment").toString, """{"todo":null}""")
  }

  "a one to one relation" should "be disconnectable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      """type Note{
              id: ID! @id
              text: String
              todo: Todo @relation(link: INLINE)
          }

          type Todo{
              id: ID! @id
              title: String!
              note: Note
          }"""
    }

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
        |}""".stripMargin,
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")
    val todoId = createResult.pathAsString("data.createNote.todo.id")

    val result = server.query(
      s"""
         |mutation {
         |  updateNote(
         |    where: { id: "$noteId"}
         |    data: { todo: { disconnect: true } }
         |  ){
         |    todo { title }
         |  }
         |}
      """,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.updateNote").toString, """{"todo":null}""")
  }

  "a one to many relation" should "be disconnectable by unique through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo{
              id: ID! @id
              title: String @unique
              comments: [Comment] $listInlineDirective
          }

          type Comment{
              id: ID! @id
              text: String @unique
              todo: Todo
          }"""
    }

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
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[{"text":"comment1"},{"text":"comment2"}]""")

    val result2 = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      title: "todo"
         |    }
         |    data:{
         |      comments: {
         |        disconnect: [{text: "comment2"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result2.pathAsJsValue("data.updateTodo.comments").toString, """[{"text":"comment1"}]""")
  }

  "A PM CM self relation" should "be disconnectable by unique through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() { s"""|type User {
                                              |  id: ID! @id
                                              |  banned: Boolean! @default(value: false)
                                              |  username: String! @unique
                                              |  password: String!
                                              |  salt: String!
                                              |  followers: [User] @relation(name: "UserFollowers" $listInlineArgument)
                                              |  follows: [User] @relation(name: "UserFollows" $listInlineArgument)
                                              |}""".stripMargin }
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
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateUser.follows").toString, """[{"username":"Peter"}]""")

    val result2 = server.query(
      s"""mutation {
         |  updateUser(
         |    where: {
         |      username: "Paul"
         |    }
         |    data:{
         |      follows: {
         |        disconnect: [{username: "Peter"}]
         |      }
         |    }
         |  ){
         |    username
         |    follows {
         |      username
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result2.pathAsJsValue("data.updateUser.follows").toString, """[]""")
  }

  "A PM CM self relation" should "should throw a correct error for disconnect on invalid unique" in {
    val project = SchemaDsl.fromStringV11() { s"""|type User {
                                              |  id: ID! @id
                                              |  banned: Boolean! @default(value: false)
                                              |  username: String! @unique
                                              |  password: String!
                                              |  salt: String!
                                              |  followers: [User] @relation(name: "UserFollowers" $listInlineArgument)
                                              |  follows: [User] @relation(name: "UserFollows" $listInlineArgument)
                                              |}""".stripMargin }
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
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateUser.follows").toString, """[{"username":"Peter"}]""")

    server.queryThatMustFail(
      s"""mutation {
         |  updateUser(
         |    where: {
         |      username: "Paul"
         |    }
         |    data:{
         |      follows: {
         |        disconnect: [{username: "Anton"}]
         |      }
         |    }
         |  ){
         |    username
         |    follows {
         |      username
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3041
//      ,
//      errorContains =
//        "The relation UserFollows has no Node for the model User with value `Paul` for username connected to a Node for the model User with value `Anton` for username"
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
                                             |}""".stripMargin }
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
      """.stripMargin

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middles: {
         |        update: [{
         |              where: {nameMiddle: "the middle"},
         |              data:{  nameMiddle: "updated middle"
         |                      bottoms: {disconnect: [{nameBottom: "the bottom"}]
         |              }
         |       }}]
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
      """.stripMargin

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
                                             |}""".stripMargin }
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
      """.stripMargin

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middles: {
         |        update: [{
         |              where: {nameMiddle: "the middle"},
         |              data:{  nameMiddle: "updated middle"
         |                      bottoms: {disconnect: [{nameBottom: "the bottom"}]
         |              }
         |       }}]
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
      """.stripMargin

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
                                             |}""".stripMargin }
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
      """.stripMargin

    server.query(createMutation, project)

    val updateMutation =
      s"""mutation b {
         |  updateTop(
         |    where: {nameTop: "the top"},
         |    data: {
         |      nameTop: "updated top",
         |      middles: {
         |        update: [{
         |              where: {nameMiddle: "the middle"},
         |              data:{  nameMiddle: "updated middle"
         |                      bottom: {disconnect: true}
         |              }
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
      """.stripMargin

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
                                             |}""".stripMargin }
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
      """.stripMargin

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
         |                update: {
         |                  nameBottom: "updated bottom"
         |                  below: { disconnect: {nameBelow: "below"}
         |
         |          }
         |                }
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
      """.stripMargin

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
                                             |}""".stripMargin }
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
      """.stripMargin

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
         |        update: {
         |              nameMiddle: "updated middle"
         |              bottom: {disconnect: true}
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
      """.stripMargin

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
                                             |}""".stripMargin }
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
      """.stripMargin

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
         |        update: {
         |              nameMiddle: "updated middle"
         |              bottom: {disconnect: true}
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
      """.stripMargin

    val result = server.query(updateMutation, project)

    result.toString should be("""{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":null}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"the bottom"}]}}""")
  }

  "Nested disconnect on self relations" should "only disconnect the specified nodes" taggedAs IgnoreMongo in {
    val project = SchemaDsl.fromStringV11() { s"""type User {
                                             |  id: ID! @id
                                             |  name: String! @unique
                                             |  follower: [User] @relation(name: "UserFollow" $listInlineArgument)
                                             |  following: [User] @relation(name: "UserFollow")
                                             |}""" }
    database.setup(project)

    server.query("""mutation  {createUser(data: {name: "X"}) {id}}""", project)
    server.query("""mutation  {createUser(data: {name: "Y"}) {id}}""", project)
    server.query("""mutation  {createUser(data: {name: "Z"}) {id}}""", project)

    val updateMutation =
      s""" mutation {
         |  updateUser(data:{
         |    following: {
         |      connect: [{ name: "Y" }, { name: "Z"}]
         |    }
         |  },where:{
         |    name:"X"
         |  }) {
         |    name
         |    following{
         |      name
         |    }
         |    follower{
         |      name
         |    }
         |  }
         |}
      """

    val result = server.query(updateMutation, project)

    result.toString should be("""{"data":{"updateUser":{"name":"X","following":[{"name":"Y"},{"name":"Z"}],"follower":[]}}}""")

    val check = server.query("""query{users{name, following{name}}}""", project)

    check.toString should be(
      """{"data":{"users":[{"name":"X","following":[{"name":"Y"},{"name":"Z"}]},{"name":"Y","following":[]},{"name":"Z","following":[]}]}}""")

    val disconnectMutation =
      s""" mutation {
         |  updateUser(data:{
         |    follower: {
         |      disconnect: [{ name: "X" }]
         |    }
         |  },where:{
         |    name:"Y"
         |  }) {
         |    name
         |    following{
         |      name
         |    }
         |  }
         |}
      """

    val result2 = server.query(disconnectMutation, project)

    result2.toString should be("""{"data":{"updateUser":{"name":"Y","following":[]}}}""")

    val result3 = server.query("""query{users{name, following{name}}}""", project)

    result3.toString should be("""{"data":{"users":[{"name":"X","following":[{"name":"Z"}]},{"name":"Y","following":[]},{"name":"Z","following":[]}]}}""")
  }

}
