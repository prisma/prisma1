package writes.nonEmbedded.nestedMutations

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NestedDeleteMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "a P1! to C1! relation " should "error when deleting the child" in {
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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
        errorCode = 0,
        errorContains = "Argument 'data' expected type 'ParentUpdateInput!'"
      )

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1! to C1 relation" should "always fail when trying to delete the child" in {
    schemaP1reqToC1opt.test { dataModel =>
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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
        errorCode = 0,
        errorContains = "Argument 'data' expected type 'ParentUpdateInput!'"
      )

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to C1  relation " should "work through a nested mutation by id" in {
    schemaP1optToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val existingDataRes = server
        .query(
          """mutation {
          |  createParent(data: {
          |    p: "existingParent"
          |    childOpt: {
          |      create: {c: "existingChild"}
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

      val existingChildId  = existingDataRes.pathAsString("data.createParent.childOpt.id")
      val existingParentId = existingDataRes.pathAsString("data.createParent.id")

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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

      // Verify existing data

      server
        .query(
          s"""
         |{
         |  parent(where:{id: "$existingParentId"}){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
          project
        )
        .toString should be(s"""{"data":{"parent":{"childOpt":{"c":"existingChild"}}}}""")
    }
  }

  "a P1 to C1  relation" should "error if the nodes are not connected" in {
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

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

    //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(1)

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  "a PM to C1!  relation " should "work" in {
    schemaPMToC1req.test { dataModel =>
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(1)
    }
  }

  "a P1 to C1!  relation " should "work" in {
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(0)
    }
  }

  "a PM to C1 " should "work" in {
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    //dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(1)
    }
  }

  "a P1! to CM  relation" should "error " in {
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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
        errorCode = 0,
        errorContains = "Argument 'data' expected type 'ParentUpdateInput!'"
      )

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a P1 to CM  relation " should "work" in {
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

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    }
  }

  // transactionality again -.-
  "a PM to CM  relation" should "work" taggedAs (IgnoreMongo) in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "otherParent"
        |    childrenOpt: {
        |      create: [{c: "otherChild"}]
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
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"},{c: "c3"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {delete: [{c: "c1"}, {c: "c2"}, {c: "otherChild"}]}
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

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c3"}]}}}""")

      server.query(s"""query{child(where:{c:"c3"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"c3","parentsOpt":[{"p":"p1"}]}}}""")

      server.query(s"""query{child(where:{c:"otherChild"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"otherChild","parentsOpt":[{"p":"otherParent"}]}}}""")

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a PM to CM  relation" should "error on invalid child" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "otherParent"
        |    childrenOpt: {
        |      create: [{c: "otherChild"}]
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
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"},{c: "c3"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      server.queryThatMustFail(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p1"}
         |  data:{
         |    childrenOpt: {delete: [{c: "c1"}, {c: "c2"}, {c: "otherChild"}]}
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

  "a PM to CM  relation" should "work for correct children" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "otherParent"
        |    childrenOpt: {
        |      create: [{c: "otherChild"}]
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
        |    p: "p1"
        |    childrenOpt: {
        |      create: [{c: "c1"},{c: "c2"},{c: "c3"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""".stripMargin,
        project
      )

      // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

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

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c3"}]}}}""")

      server.query(s"""query{parents {p, childrenOpt{c}}}""", project).toString should be(
        """{"data":{"parents":[{"p":"otherParent","childrenOpt":[{"c":"otherChild"}]},{"p":"p1","childrenOpt":[{"c":"c3"}]}]}}""")

      server.query(s"""query{child(where:{c:"c3"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"c3","parentsOpt":[{"p":"p1"}]}}}""")

      server.query(s"""query{child(where:{c:"otherChild"}){c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"child":{"c":"otherChild","parentsOpt":[{"p":"otherParent"}]}}}""")

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a PM to CM relation" should "delete fail if other req relations would be violated" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Parent{
        | id: ID! @id
        | p: String! @unique
        | childrenOpt: [Child] $listInlineDirective
        |}
        |
        |type Child{
        | id: ID! @id
        | c: String! @unique
        | parentsOpt: [Parent]
        | otherReq: Other! @relation(link: INLINE)
        |}
        |
        |type Other{
        | id: ID! @id
        | o: String! @unique
        | childReq: Child!
        |}
      """
    }

    database.setup(project)

    server.query(
      """mutation {
        |  createOther(data: {
        |    o: "o1"
        |    childReq: {
        |      create: {c: "c1"}
        |    }
        |  }){
        |    o
        |  }
        |}""".stripMargin,
      project
    )

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToOther").await should be(1) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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
      errorContains = """The change you are trying to make would violate the required relation 'ChildToOther' between Child and Other"""
    )

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToOther").await should be(1) }
  }

  "a PM to CM  relation" should "delete the child from other relations as well" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Parent{
        | id: ID! @id
        | p: String! @unique
        | childrenOpt: [Child] $listInlineDirective
        |}
        |
        |type Child{
        | id: ID! @id
        | c: String! @unique
        | parentsOpt: [Parent]
        | otherOpt: Other @relation(link: INLINE)
        |}
        |
        |type Other{
        | id: ID! @id
        | o: String! @unique
        | childOpt: Child
        |}
      """
    }

    database.setup(project)

    server.query(
      """mutation {
        |  createOther(data: {
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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToOther").await should be(1) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

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

    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }
    // ifConnectorIsActive { dataResolver(project).countByTable("_ChildToOther").await should be(0) }
  }

  "a one to many relation" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Todo{
        | id: ID! @id
        | comments: [Comment] $listInlineDirective
        |}
        |
        |type Comment{
        | id: ID! @id
        | text: String
        | todo: Todo
        |}
      """
    }

    database.setup(project)

    val otherCommentId = server
      .query(
        """mutation {
        |  createComment(
        |    data: {
        |      text: "otherComment"
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
        project
      )
      .pathAsString("data.createComment.id")

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

    server.queryThatMustFail(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        delete: [{id: "$otherCommentId"}]
         |      }
         |    }
         |  ){
         |    comments {
         |      text
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      3041
    )

    mustBeEqual(result.pathAsJsValue("data.updateTodo.comments").toString, """[]""")

    val query = server.query("""{ comments { text }}""", project)
    mustBeEqual(query.toString, """{"data":{"comments":[{"text":"otherComment"}]}}""")

  }

  "a one to many relation" should "be deletable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Todo{
        | id: ID! @id
        | comments: [Comment] $listInlineDirective
        |}
        |
        |type Comment{
        | id: ID! @id
        | text: String
        | alias: String! @unique
        | todo: Todo
        |}
      """
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

  }

  "a many to one relation" should "be deletable by id through a nested mutation" in {

    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Todo{
        | id: ID! @id
        | comments: [Comment] $listInlineDirective
        |}
        |
        |type Comment{
        | id: ID! @id
        | text: String
        | todo: Todo
        |}
      """
    }

    database.setup(project)

    val existingCreateResult = server.query(
      """mutation {
        |  createTodo(
        |    data: {
        |      comments: {
        |        create: [{text: "otherComment"}]
        |      }
        |    }
        |  ){
        |    id
        |    comments { id }
        |  }
        |}""".stripMargin,
      project
    )
    val existingTodoId    = existingCreateResult.pathAsString("data.createTodo.id")
    val existingCommentId = existingCreateResult.pathAsString("data.createTodo.comments.[0].id")

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

    val query = server.query("""{ todoes { id comments { id } }}""", project)
    mustBeEqual(query.toString, s"""{"data":{"todoes":[{"id":"$existingTodoId","comments":[{"id":"$existingCommentId"}]}]}}""")
  }

  "one2one relation both exist and are connected" should "be deletable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo{
        | id: ID! @id
        | title: String
        | note: Note @relation(link: INLINE)
        |}
        |
        |type Note{
        | id: ID! @id
        | text: String
        | todo: Todo
        |}
      """
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
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo{
        | id: ID! @id
        | title: String! @unique
        | note: Note @relation(link: INLINE)
        |}
        |
        |type Note{
        | id: ID! @id
        | text: String! @unique
        | todo: Todo
        |}
      """
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
    val project = SchemaDsl.fromStringV11() {
      """
        |type Todo{
        | id: ID! @id
        | title: String
        | note: Note @relation(link: INLINE)
        |}
        |
        |type Note{
        | id: ID! @id
        | text: String
        | todo: Todo
        |}
      """
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
        s"The relation NoteToTodo has no node for the model Note with the value '$noteId' for the field 'id' connected to a node for the model Todo on your mutation path."
    )

    val query = server.query("""{ todoes { title }}""", project)
    mustBeEqual(query.toString, """{"data":{"todoes":[]}}""")

    val query2 = server.query("""{ notes { text }}""", project)
    mustBeEqual(query2.toString, """{"data":{"notes":[{"text":"Note"}]}}""")
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

  "Nested delete on self relations" should "only delete the specified nodes" in {
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

    val deleteMutation =
      s""" mutation {
         |  updateUser(data:{
         |    follower: {
         |      delete: [{ name: "X" }]
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

    val result2 = server.query(deleteMutation, project)

    result2.toString should be("""{"data":{"updateUser":{"name":"Y","following":[]}}}""")

    val result3 = server.query("""query{users{name, following{name}}}""", project)

    result3.toString should be("""{"data":{"users":[{"name":"Y","following":[]},{"name":"Z","following":[]}]}}""")
  }
}
