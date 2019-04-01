package com.prisma.api.mutations.nonEmbedded.nestedMutations

import com.prisma.IgnoreSQLite
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedConnectMutationInsideCreateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  "a P1! to C1! relation with the child already in a relation" should "error when connecting by id since old required parent relation would be broken" in {
    schemaP1reqToC1req.test { dataModel =>
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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      server.queryThatMustFail(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childReq: {connect: {id: "$child1Id"}}
           |  }){
           |    childReq {
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

  "a P1! to C1 relation with the child already in a relation" should "should fail on existing old parent" in {
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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      server.queryThatMustFail(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childReq: {connect: {id: "$child1Id"}}
           |  }){
           |    childReq {
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

  "a P1! to C1  relation with the child not in a relation" should "be connectable through a nested mutation by id" in {
    schemaP1reqToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val looseChildId = server
        .query(
          """mutation {
          |  createChild(data: {c: "looseChild"})
          |  {
          |    id
          |  }
          |}""".stripMargin,
          project
        )
        .pathAsString("data.createChild.id")

      val otherParentWithChildId = server
        .query(
          s"""
           |mutation {
           |  createParent(data:{
           |    p: "otherParent"
           |    childReq: {create: {c: "otherChild"}}
           |  }){
           |    id
           |  }
           |}
      """.stripMargin,
          project
        )
        .pathAsString("data.createParent.id")

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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      val res = server.query(
        s"""
         |mutation {
         |  createParent(data:{
         |    p: "p2"
         |    childReq: {connect: {id: "$child1Id"}}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childReq":{"c":"c1"}}}}""")
      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      // verify preexisting data

      server
        .query(
          s"""
           |{
           |  parent(where: {id: "$otherParentWithChildId"}){
           |    childReq {
           |      c
           |    }
           |  }
           |}
      """.stripMargin,
          project
        )
        .pathAsString("data.parent.childReq.c") should be("otherChild")

      server
        .query(
          s"""
           |{
           |  child(where: {id: "$looseChildId"}){
           |    c
           |  }
           |}
      """.stripMargin,
          project
        )
        .pathAsString("data.child.c") should be("looseChild")
    }
  }

  "a P1 to C1  relation with the child already in a relation" should "be connectable through a nested mutation by id if the child is already in a relation" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val child1Id = server
        .query(
          """mutation {
            |  createParent(data: {
            |    p: "p1"
            |    childOpt: {
            |      create: {c: "c1"}
            |    }
            |  }){
            |    childOpt{
            |       id
            |    }
            |  }
            |}""".stripMargin,
          project
        )
        .pathAsString("data.createParent.childOpt.id")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childOpt: {connect: {id: "$child1Id"}}
           |  }){
           |    childOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childOpt":{"c":"c1"}}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to C1  relation with the child without a relation" should "be connectable through a nested mutation by id" in {
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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childOpt: {connect: {id: "$child1Id"}}
           |  }){
           |    childOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childOpt":{"c":"c1"}}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    }
  }

  "a PM to C1!  relation with the child already in a relation" should "be connectable through a nested mutation by unique" in {
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

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childrenOpt: {connect: {c: "c1"}}
           |  }){
           |    childrenOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to C1!  relation with the child already in a relation" should "be connectable through a nested mutation by unique" in {
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

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childOpt: {connect: {c: "c1"}}
           |  }){
           |    childOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childOpt":{"c":"c1"}}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to C1  relation with the child already in a relation" should "be connectable through a nested mutation by unique" in {
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

      // we are even resilient against multiple identical connects here -> twice connecting to c2

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childrenOpt: {connect: [{c: "c1"},{c: "c2"},{c: "c2"}]}
           |  }){
           |    childrenOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a PM to C1  relation with the child without a relation" should "be connectable through a nested mutation by unique" in {
    schemaPMToC1opt.test { dataModel =>
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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childrenOpt: {connect: {c: "c1"}}
           |  }){
           |    childrenOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to C1  relation with a child without a relation" should "error if also trying to connect to a non-existing node" taggedAs IgnoreSQLite in { // TODO: Remove when transactions are back
    schemaPMToC1opt.test { dataModel =>
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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      server.queryThatMustFail(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childrenOpt: {connect: [{c: "c1"}, {c: "DOES NOT EXIST"}]}
           |  }){
           |    childrenOpt {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project,
        errorCode = 3039,
        errorContains = "No Node for the model Child with value DOES NOT EXIST for c found."
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  "a P1! to CM  relation with the child already in a relation" should "be connectable through a nested mutation by unique" in {
    schemaP1reqToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childReq: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    childReq{
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
         |  createParent(data:{
         |    p: "p2"
         |    childReq: {connect: {c: "c1"}}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childReq":{"c":"c1"}}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p1"},{"p":"p2"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a P1! to CM  relation with the child not already in a relation" should "be connectable through a nested mutation by unique" in {
    schemaP1reqToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
          |  createChild(data: {c: "c1"}){
          |       c
          |  }
          |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
           |mutation {
           |  createParent(data:{
           |    p: "p2"
           |    childReq: {connect: {c: "c1"}}
           |  }){
           |    childReq {
           |      c
           |    }
           |  }
           |}
        """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childReq":{"c":"c1"}}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p2"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    }
  }

  "a P1 to CM  relation with the child already in a relation" should "be connectable through a nested mutation by unique" in {
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
         |  createParent(data:{
         |    p: "p2"
         |    childOpt: {connect: {c: "c1"}}
         |  }){
         |    childOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childOpt":{"c":"c1"}}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p1"},{"p":"p2"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a P1 to CM  relation with the child not already in a relation" should "be connectable through a nested mutation by unique" in {
    schemaP1optToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createChild(data: {c: "c1"}){
        |       c
        |  }
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
         |mutation {
         |  createParent(data:{
         |    p: "p2"
         |    childOpt: {connect: {c: "c1"}}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childOpt":{"c":"c1"}}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p2"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be connectable through a nested mutation by unique" in {
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

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      val res = server.query(
        s"""
         |mutation {
         |  createParent(data:{
         |    p: "p2"
         |    childrenOpt: {connect: [{c: "c1"}, {c: "c2"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be(
        """{"data":{"children":[{"parentsOpt":[{"p":"p1"},{"p":"p2"}]},{"parentsOpt":[{"p":"p1"},{"p":"p2"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }
    }
  }

  "a PM to CM  relation with the child not already in a relation" should "be connectable through a nested mutation by unique" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createChild(data: {c: "c1"}){
        |       c
        |  }
        |}""".stripMargin,
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
         |mutation {
         |  createParent(data:{
         |    p: "p2"
         |    childrenOpt: {connect: {c: "c1"}}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
        project
      )

      res.toString should be("""{"data":{"createParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p2"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    }
  }

  "a PM to CM  relation without a backrelation" should "be connectable through a nested mutation by unique" in {
    val project = SchemaDsl.fromString() {
      """type Role {
        | id: ID! @unique
        | r: String! @unique
        |}
        |
        |type User {
        | id: ID! @unique
        | u: String! @unique
        | roles: [Role]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query(
      """mutation {
        |  createRole(data: {r: "r1"}){
        |       r
        |  }
        |}""".stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_RoleToUser").await should be(0) }

    val res = server.query(
      s"""
         |mutation {
         |  createUser(data:{
         |    u: "u2"
         |    roles: {connect: {r: "r1"}}
         |  }){
         |    roles {
         |      r
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"createUser":{"roles":[{"r":"r1"}]}}}""")

    ifConnectorIsActive { dataResolver(project).countByTable("_RoleToUser").await should be(1) }
  }

  "a many relation" should "be connectable through a nested mutation by id" in {
    val project = SchemaDsl.fromString() {
      """type Comment {
        | id: ID! @unique
        | text: String!
        | todo: Todo
        |}
        |
        |type Todo {
        | id: ID! @unique
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    val comment1Id = server.query("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    val comment2Id = server.query("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.query(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
         |    }
         |  }){
         |    id
         |    comments {
         |      id
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      actual = result.pathAsJsValue("data.createTodo.comments").toString,
      expected = s"""[{"id":"$comment1Id","text":"comment1"},{"id":"$comment2Id","text":"comment2"}]"""
    )
  }

  "a many relation" should "throw a proper error if connected by wrong id" in {
    val project = SchemaDsl.fromString() {
      """type Comment {
        | id: ID! @unique
        | text: String!
        | todo: Todo
        |}
        |
        |type Todo {
        | id: ID! @unique
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.queryThatMustFail(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{id: "5beea4aa6183dd734b2dbd9b"}]
         |    }
         |  }){
         |    id
         |    comments {
         |      id
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Comment with value 5beea4aa6183dd734b2dbd9b for id found."
    )
  }

  "a many relation" should "throw a proper error if connected by wrong id the other way around" in {
    val project = SchemaDsl.fromString() {
      """type Comment {
        | id: ID! @unique
        | text: String!
        | todo: Todo
        |}
        |
        |type Todo {
        | id: ID! @unique
        | text: String
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    server.queryThatMustFail(
      s"""
         |mutation {
         |  createComment(data:{
         |    text: "bla"
         |    todo: {
         |      connect: {id: "5beea4aa6183dd734b2dbd9b"}
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Todo with value 5beea4aa6183dd734b2dbd9b for id found."
    )
  }

  "a many relation" should "throw a proper error if the id of a wrong model is provided" in {
    val project = SchemaDsl.fromString() {
      """type Comment {
        | id: ID! @unique
        | text: String!
        | todo: Todo
        |}
        |
        |type Todo {
        | id: ID! @unique
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    val comment1Id = server.query("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    val comment2Id = server.query("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val todoId = server
      .query(
        s"""
           |mutation {
           |  createTodo(data:{
           |    comments: {
           |      connect: [{id: "$comment1Id"}, {id: "$comment2Id"}]
           |    }
           |  }){
           |    id
           |  }
           |}
      """.stripMargin,
        project
      )
      .pathAsString("data.createTodo.id")

    server.queryThatMustFail(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{id: "$todoId"}]
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = s"No Node for the model Comment with value $todoId for id found."
    )

  }

  "a many relation" should "be connectable through a nested mutation by any unique argument" in {
    val project = SchemaDsl.fromString() {
      """type Comment {
        | id: ID! @unique
        | text: String!
        | alias: String! @unique
        | todo: Todo
        |}
        |
        |type Todo {
        | id: ID! @unique
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    val comment1Alias = server
      .query("""mutation { createComment(data: {text: "text comment1", alias: "comment1"}){ alias } }""", project)
      .pathAsString("data.createComment.alias")
    val comment2Alias = server
      .query("""mutation { createComment(data: {text: "text comment2", alias: "comment2"}){ alias } }""", project)
      .pathAsString("data.createComment.alias")

    val result = server.query(
      s"""
         |mutation {
         |  createTodo(data:{
         |    comments: {
         |      connect: [{alias: "$comment1Alias"}, {alias: "$comment2Alias"}]
         |    }
         |  }){
         |    id
         |    comments {
         |      alias
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      actual = result.pathAsJsValue("data.createTodo.comments").toString,
      expected = s"""[{"alias":"$comment1Alias","text":"text comment1"},{"alias":"$comment2Alias","text":"text comment2"}]"""
    )
  }

  "a many relation" should "be connectable through a nested mutation by any unique argument in the opposite direction" in {
    val project = SchemaDsl.fromString() {
      """type Comment {
        | id: ID! @unique
        | todo: Todo
        |}
        |
        |type Todo {
        | id: ID! @unique
        | title: String!
        | alias: String! @unique
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    val todoAlias = server
      .query("""mutation { createTodo(data: {title: "the title", alias: "todo1"}){ alias } }""", project)
      .pathAsString("data.createTodo.alias")

    val result = server.query(
      s"""
         |mutation {
         |  createComment(
         |    data: {
         |      todo: {
         |        connect: { alias: "$todoAlias"}
         |      }
         |    }
         |  )
         |  {
         |    todo {
         |      alias
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(
      actual = result.pathAsJsValue("data.createComment.todo").toString,
      expected = s"""{"alias":"$todoAlias","title":"the title"}"""
    )
  }
}
