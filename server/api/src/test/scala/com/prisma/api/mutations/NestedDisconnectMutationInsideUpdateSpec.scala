package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.api.database.DatabaseQueryBuilder
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedDisconnectMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiBaseSpec {

  "a P1! to C1! relation with the child already in a relation" should "error when disconnecting by id since old required parent relation would be broken" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).oneToOneRelation_!("parentReq", "childReq", parent)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {disconnect: {id: "$childId"}}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_ChildToParent' between Child and Parent"
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1! to C1 relation" should "always fail when trying to disconnect" in {
    val project = SchemaDsl() { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation_!("childReq", "parentOpt", child, isRequiredOnFieldB = false)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childReq: {disconnect: {id: "$childId"}}
         |  }){
         |    childReq {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_ParentToChild' between Parent and Child"
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1 to C1  relation " should "be disconnectable through a nested mutation by id" in {
    val project = SchemaDsl() { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation("childOpt", "parentOpt", child)
    }
    database.setup(project)

    val res = server
      .executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    val res2 = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    p: "p2"
         |    childOpt: {disconnect: {id: "$childId"}}
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(0))
  }

  "a P1 to C1  relation with the child and the parent without a relation" should "not be disconnectable through a nested mutation by id" in {
    val project = SchemaDsl() { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToOneRelation("childOpt", "parentOpt", child)
    }
    database.setup(project)

    val child1Id = server
      .executeQuerySimple(
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
      .executeQuerySimple(
        """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""".stripMargin,
        project
      )
      .pathAsString("data.createParent.id")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(0))

    val res = server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parent1Id"}
         |  data:{
         |    p: "p2"
         |    childOpt: {disconnect: {id: "$child1Id"}}
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(0))
  }

  "a PM to C1!  relation with the child already in a relation" should "not be disconnectable through a nested mutation by unique" in {
    val project = SchemaDsl() { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToManyRelation_!("childrenOpt", "parentReq", child)
    }
    database.setup(project)

    server.executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1 to C1!  relation with the child and the parent already in a relation" should "should error in a nested mutation by unique" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      schema.model("Child").field_!("c", _.String, isUnique = true).oneToOneRelation_!("parentReq", "childOpt", parent, isRequiredOnFieldB = false)
    }
    database.setup(project)

    server.executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childOpt: {disconnect: {c: "c1"}}
         |  }){
         |    childOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3042,
      errorContains = "The change you are trying to make would violate the required relation '_ChildToParent' between Child and Parent"
    )

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a PM to C1  relation with the child already in a relation" should "be disconnectable through a nested mutation by unique" in {
    val project = SchemaDsl() { schema =>
      val child = schema.model("Child").field_!("c", _.String, isUnique = true)
      schema.model("Parent").field_!("p", _.String, isUnique = true).oneToManyRelation("childrenOpt", "parentOpt", child)
    }
    database.setup(project)

    server
      .executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(2))

    val res = server.executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ParentToChild").as[Int]) should be(Vector(1))
  }

  "a P1! to CM  relation with the child already in a relation" should "error through a nested mutation by unique" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).oneToManyRelation_!("parentsOpt", "childReq", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    server.executeQuerySimpleThatMustFail(
      s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childReq: {disconnect: {c: "c1"}}
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a P1 to CM  relation with the child already in a relation" should "be disconnectable through a nested mutation by unique" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).oneToManyRelation("parentsOpt", "childOpt", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))

    val res = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childOpt: {disconnect: {c: "c1"}}
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

    server.executeQuerySimple(s"""query{children{c, parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"c":"c1","parentsOpt":[]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(0))
  }

  "a PM to CM  relation with the children already in a relation" should "be disconnectable through a nested mutation by unique" in {
    val project = SchemaDsl() { schema =>
      val parent = schema.model("Parent").field_!("p", _.String, isUnique = true)
      val child  = schema.model("Child").field_!("c", _.String, isUnique = true).manyToManyRelation("parentsOpt", "childrenOpt", parent)
    }
    database.setup(project)

    server.executeQuerySimple(
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

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(2))

    val res = server.executeQuerySimple(
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

    server.executeQuerySimple(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"c1","parentsOpt":[]},{"c":"c2","parentsOpt":[{"p":"p1"}]}]}}""")

    database.runDbActionOnClientDb(DatabaseQueryBuilder.itemCountForTable(project.id, "_ChildToParent").as[Int]) should be(Vector(1))
  }

  "a one to many relation" should "be disconnectable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
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

    val result = server.executeQuerySimple(
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
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String).field_!("alias", _.String, isUnique = true)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
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

    val result = server.executeQuerySimple(
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
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String)
      schema.model("Todo").oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
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

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateComment(
         |    where: {
         |      id: "$commentId"
         |    }
         |    data: {
         |      todo: {
         |        disconnect: {id: "$todoId"}
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
  }

  "a one to one relation" should "be disconnectable by id through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val note = schema.model("Note").field("text", _.String)
      schema.model("Todo").field_!("title", _.String).oneToOneRelation("note", "todo", note)
    }
    database.setup(project)

    val createResult = server.executeQuerySimple(
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

    val result = server.executeQuerySimple(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      todo: {
         |        disconnect: {id: "$todoId"}
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
  }

  "a one to many relation" should "be disconnectable by unique through a nested mutation" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field("text", _.String, isUnique = true)
      schema.model("Todo").field("title", _.String, isUnique = true).oneToManyRelation("comments", "todo", comment)
    }
    database.setup(project)

    server.executeQuerySimple("""mutation { createTodo(data: {title: "todo"}){ id } }""", project).pathAsString("data.createTodo.id")
    server.executeQuerySimple("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    server.executeQuerySimple("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.executeQuerySimple(
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

    val result2 = server.executeQuerySimple(
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
}
