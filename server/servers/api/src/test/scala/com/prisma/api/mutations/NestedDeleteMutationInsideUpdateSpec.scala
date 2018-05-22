package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedDeleteMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase {

  "a P1! to C1! relation " should "error when deleting the child" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).oneToOneRelation_!("parentReq", "childReq", parent)
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
          |    id
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )
    val childId  = res.pathAsString("data.createParent.childReq.id")
    val parentId = res.pathAsString("data.createParent.id")

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {delete: true}
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

  "a P1! to C1 relation" should "always fail when trying to delete the child" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation_!("childReq", "parentOpt", child, isRequiredOnFieldB = false)
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
          |  id
          |    childReq{
          |       id
          |    }
          |  }
          |}""".stripMargin,
        project
      )

    val childId  = res.pathAsString("data.createParent.childReq.id")
    val parentId = res.pathAsString("data.createParent.id")

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(1) }

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {delete: true}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation 'ParentToChild' between Parent and Child"
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(1) }
  }

  "a P1 to C1  relation " should "work through a nested mutation by id" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation("childOpt", "parentOpt", child)
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

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(1) }

    val res2 = server.query(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childOpt: {delete: true}
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

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(0) }
  }

  "a P1 to C1  relation" should "error if the nodes are not connected" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation("childOpt", "parentOpt", child)
    }
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

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(0) }

    val res = server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {delete: true}
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

    dataResolver(project).countByTable("Parent").await should be(1)
    dataResolver(project).countByTable("Child").await should be(1)

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(0) }
  }

  "a PM to C1!  relation " should "work" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToManyRelation_!("childrenOpt", "parentReq", child)
    }
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

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(1) }

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {delete: {c: "c1"}}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(0) }
    dataResolver(project).countByTable("Parent").await should be(1)
    dataResolver(project).countByTable("Child").await should be(0)
  }

  "a P1 to C1!  relation " should "work" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      schema.model("Child").field_!("c", _.String, isUnique = true).oneToOneRelation_!("parentReq", "childOpt", parent, isRequiredOnFieldB = false)
    }
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

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childOpt: {delete: true}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    dataResolver(project).countByTable("Parent").await should be(1)
    dataResolver(project).countByTable("Child").await should be(0)
  }

  "a PM to C1 " should "work" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToManyRelation("childrenOpt", "parentOpt", child)
    }
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

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(2) }

    val res = server.query(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {delete: [{c: "c2"}]}
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

    ifConnectorIsActive { dataResolver(project).countByTable("_ParentToChild").await should be(1) }
    dataResolver(project).countByTable("Parent").await should be(1)
    dataResolver(project).countByTable("Child").await should be(1)
  }

  "a P1! to CM  relation" should "error " in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).oneToManyRelation_!("parentsOpt", "childReq", parent)
    }
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

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childReq: {delete: true}
         |  }){
         |    childReq {
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

  "a P1 to CM  relation " should "work" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).oneToManyRelation("parentsOpt", "childOpt", parent)
    }
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
         |    childOpt: {delete: true}
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

    server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[]}}""")

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
  }

  "a PM to CM  relation" should "work" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).manyToManyRelation("parentsOpt", "childrenOpt", parent)
    }
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
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {delete: [{c: "c1"}, {c: "c2"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[]}}}""")

    server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[]}}""")

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
  }

  "a PM to CM  relation" should "delete fail if other req relations would be violated" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent   = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child    = schema.model("Child").field_!("c", _.String, isUnique = true).manyToManyRelation("parentsOpt", "childrenOpt", parent)
      val reqOther = schema.model("ReqOther").field_!("r", _.String, isUnique = true).oneToOneRelation_!("childReq", "otherReq", child)
    }
    database.setup(project)

    server.query(
      """mutation {
        |  createReqOther(data: {
        |    r: "r1"
        |    childReq: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    r
        |  }
        |}""".stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ReqOtherToChild").await should be(1) }

    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      connect: {c: "c1"}
        |    }
        |  }){
        |    p
        |  }
        |}""".stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {delete: [{c: "c1"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = """The change you are trying to make would violate the required relation 'ReqOtherToChild' between ReqOther and Child"""
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    ifConnectorIsActive { dataResolver(project).countByTable("_ReqOtherToChild").await should be(1) }

  }

  "a PM to CM  relation" should "delete the child from other relations as well" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val parent   = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child    = schema.model("Child").field_!("c", _.String, isUnique = true).manyToManyRelation("parentsOpt", "childrenOpt", parent)
      val optOther = schema.model("OptOther").field_!("o", _.String, isUnique = true).oneToOneRelation("childOpt", "otherOpt", child)

    }
    database.setup(project)

    server.query(
      """mutation {
        |  createOptOther(data: {
        |    o: "o1"
        |    childOpt: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    o
        |  }
        |}""".stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_OptOtherToChild").await should be(1) }

    server.query(
      """mutation {
        |  createParent(data: {
        |    p: "p1"
        |    childrenOpt: {
        |      connect: {c: "c1"}
        |    }
        |  }){
        |    p
        |  }
        |}""".stripMargin,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    val res = server.query(
      s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {delete: [{c: "c1"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[]}}}""")

    server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[]}}""")

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    ifConnectorIsActive { dataResolver(project).countByTable("_OptOtherToChild").await should be(0) }
  }

  "a one to many relation" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
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
         |        delete: [{id: "$comment1Id"}, {id: "$comment2Id"}]
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

    val query = server.query("""{ comments { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"comments":[]}}""")
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(1) }
  }

  "a one to many relation" should "be deletable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val comment = schema.model("Comment").field("text", _.String).field_!("alias", _.String, isUnique = true)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
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
         |        delete: [{alias: "alias1"}, {alias: "alias2"}]
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

    val query = server.query("""{ comments { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"comments":[]}}""")
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(1) }
  }

  "a many to one relation" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
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
         |      todo: {
         |        delete: true
         |      }
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

    val query = server.query("""{ todoes { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")
  }

  "one2one relation both exist and are connected" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
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
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      todo: {
         |        delete: true
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.updateNote").toString, """{"todo":null}""")

    val query = server.query("""{ todoes { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")
  }

  "one2one relation both exist and are connected" should "be deletable by unique field through a nested mutation" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val note = schema.model("Note").field("text", _.String, isUnique = true)
      schema.model("Todo").field_!("title", _.String, isUnique = true).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "FirstUnique"
        |      todo: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )

    val result = server.query(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      text: "FirstUnique"
         |    }
         |    data: {
         |      todo: {
         |        delete: true
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsJsValue("data.updateNote").toString, """{"todo":null}""")

    val query = server.query("""{ todoes { id }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")

    val query2 = server.query("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"FirstUnique"}]}}""")
  }

  "a one to one relation" should "not do a nested delete by id if the nested node does not exist" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Note"
        |    }
        |  ){
        |    id
        |    todo { id }
        |  }
        |}""".stripMargin,
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")

    val result = server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {id: "$noteId"}
         |    data: {
         |      todo: {
         |        delete: true
         |      }
         |    }
         |  ){
         |    todo {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3041,
      errorContains =
        s"The relation TodoToNote has no node for the model Note with the value '$noteId' for the field 'id' connected to a node for the model Todo on your mutation path."
    )

    val query = server.query("""{ todoes { title }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")

    val query2 = server.query("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"Note"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle!]!
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top!]!
                                             |  bottoms: [Bottom!]!
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
                                             |  nameBottom: String! @unique
                                             |  middles: [Middle!]!
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
         |                      bottoms: {delete: [{nameBottom: "the bottom"}]
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
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path and there are no backrelations" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle!]!
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  bottoms: [Bottom!]!
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
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
         |                      bottoms: {delete: [{nameBottom: "the bottom"}]
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
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path " in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle!]!
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top!]!
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
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
         |                      bottom: {delete: true}
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
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path  and back relations are missing and node edges follow model edges" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
                                             |  nameBottom: String! @unique
                                             |  below: [Below!]!
                                             |}
                                             |
                                             |type Below {
                                             |  id: ID! @unique
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
         |                  below: { delete: {nameBelow: "below"}
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
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  top: Top
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
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
         |              bottom: {delete: true}
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
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path and there are no backrelations" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
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
         |              bottom: {delete: true}
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
  }

}
