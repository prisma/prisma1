package com.prisma.api.mutations.nonEmbedded.nestedMutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.{JoinRelationLinksCapability, RelationLinkListCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedSetMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  "a PM to C1!  relation with the child already in a relation" should "be setable through a nested mutation by unique" in {
    schemaPMToC1req.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      val otherParentWithChildId = server
        .query(
          s"""
           |mutation {
           |  createParent(data:{
           |    p: "otherParent"
           |    childrenOpt: {create: {c: "otherChild"}}
           |  }){
           |    id
           |  }
           |}
      """.stripMargin,
          project
        )
        .pathAsString("data.createParent.id")

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

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    childrenOpt: {
        |      create: {c: "c2"}
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(3) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |    where: {p: "p2"}
         |    data:{
         |    childrenOpt: {set: {c: "c1"}}
         |  }){
         |    childrenOpt(first:10) {
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      // verify preexisting data

      server
        .query(
          s"""
           |{
           |  parent(where: {id: "${otherParentWithChildId}"}){
           |    childrenOpt {
           |      c
           |    }
           |  }
           |}
      """.stripMargin,
          project
        )
        .pathAsString("data.parent.childrenOpt.[0].c") should be("otherChild")
    }
  }

  "a PM to C1  relation with the child already in a relation" should "be setable through a nested mutation by unique" in {
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

      server
        .query(
          """mutation {
          |  createParent(data: {p: "p2"}){
          |    p
          |  }
          |}""",
          project
        )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

      // we are even resilient against multiple identical connects here -> twice connecting to c2

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: { p: "p2"}
         |  data:{
         |    childrenOpt: {set: [{c: "c1"},{c: "c2"},{c: "c2"}]}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a PM to C1  relation with the child without a relation" should "be setable through a nested mutation by unique" in {
    schemaPMToC1opt.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server
        .query(
          """mutation {
          |  createChild(data: {c: "c1"})
          |  {
          |    id
          |  }
          |}""",
          project
        )

      server
        .query(
          """mutation {
          |  createParent(data: {p: "p1"})
          |  {
          |    id
          |  }
          |}""",
          project
        )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: {p:"p1"}
         |  data:{
         |    childrenOpt: {set: {c: "c1"}}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be setable through a nested mutation by unique" in {
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

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    childrenOpt: {
        |      create: [{c: "c3"},{c: "c4"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: {    p: "p2"}
         |  data:{
         |    childrenOpt: {set: [{c: "c1"}, {c: "c2"}]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

      server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"},{"p":"p2"}]},{"c":"c2","parentsOpt":[{"p":"p1"},{"p":"p2"}]},{"c":"c3","parentsOpt":[]},{"c":"c4","parentsOpt":[]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }
    }
  }

  "a PM to CM  relation with the child not already in a relation" should "be setable through a nested mutation by unique" in {
    schemaPMToCM.test { dataModel =>
      val project = SchemaDsl.fromStringV11() { dataModel }
      database.setup(project)

      server.query(
        """mutation {
        |  createChild(data: {c: "c1"}){
        |       c
        |  }
        |}""",
        project
      )

      server.query(
        """mutation {
        |  createParent(data: {p: "p1"}){
        |       p
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(0) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: {p: "p1"}
         |  data:{
         |    childrenOpt: {set: {c: "c1"}}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"}]}}}""")

      server.query(s"""query{children{parentsOpt{p}}}""", project).toString should be("""{"data":{"children":[{"parentsOpt":[{"p":"p1"}]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    }
  }

  "a PM to CM  relation with the children already in a relation" should "be setable to empty" in {
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

      server.query(
        """mutation {
        |  createParent(data: {
        |    p: "p2"
        |    childrenOpt: {
        |      create: [{c: "c3"},{c: "c4"}]
        |    }
        |  }){
        |    childrenOpt{
        |       c
        |    }
        |  }
        |}""",
        project
      )

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(4) }

      val res = server.query(
        s"""
         |mutation {
         |  updateParent(
         |  where: {    p: "p2"}
         |  data:{
         |    childrenOpt: {set: []}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """,
        project
      )

      res.toString should be("""{"data":{"updateParent":{"childrenOpt":[]}}}""")

      server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
        """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"}]},{"c":"c2","parentsOpt":[{"p":"p1"}]},{"c":"c3","parentsOpt":[]},{"c":"c4","parentsOpt":[]}]}}""")

      ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    }
  }

  "a one to many relation" should "be setable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      """type Comment {
        | id: ID! @id
        | text: String
        | todo: Todo @relation(link: INLINE)
        |}
        |
        |type Todo {
        | id: ID! @id
        | comments: [Comment]
        |}
      """.stripMargin
    }
    database.setup(project)

    val todoId     = server.query("""mutation { createTodo(data: {}){ id } }""", project).pathAsString("data.createTodo.id")
    val comment1Id = server.query("""mutation { createComment(data: {text: "comment1"}){ id } }""", project).pathAsString("data.createComment.id")
    val comment2Id = server.query("""mutation { createComment(data: {text: "comment2"}){ id } }""", project).pathAsString("data.createComment.id")

    val result = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        set: [{id: "$comment1Id"}, {id: "$comment2Id"}]
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
  }

  "a one to many relation" should "be setable by unique through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      """type Comment {
        | id: ID! @id
        | text: String @unique
        | todo: Todo @relation(link: INLINE)
        |}
        |
        |type Todo {
        | id: ID! @id
        | title: String @unique
        | comments: [Comment]
        |}
      """.stripMargin
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
         |        set: [{text: "comment1"}, {text: "comment2"}]
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
  }

  "a PM to CM  self relation with the child not already in a relation" should "be setable through a nested mutation by unique" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Technology {
         |  id: ID! @id
         |  name: String! @unique
         |  childTechnologies: [Technology] @relation(name: "ChildTechnologies" $listInlineArgument)
         |  parentTechnologies: [Technology] @relation(name: "ChildTechnologies")
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query("""mutation {createTechnology(data: {name: "techA"}){name}}""", project)
    server.query("""mutation {createTechnology(data: {name: "techB"}){name}}""", project)

    val res = server.query(
      s"""mutation {
         |  updateTechnology(where: {name: "techA"},
         |                   data:  {childTechnologies: {set: {name: "techB"}}})
         |      {name,
         |       childTechnologies  {name}
         |       parentTechnologies {name}}
         |}
      """,
      project
    )

    res.toString should be("""{"data":{"updateTechnology":{"name":"techA","childTechnologies":[{"name":"techB"}],"parentTechnologies":[]}}}""")

    val res2 = server.query(
      s"""query {
         |  technologies{
         |       name
         |       childTechnologies  {name}
         |       parentTechnologies {name}
         |  }
         |}
      """,
      project
    )

    res2.toString should be(
      """{"data":{"technologies":[{"name":"techA","childTechnologies":[{"name":"techB"}],"parentTechnologies":[]},{"name":"techB","childTechnologies":[],"parentTechnologies":[{"name":"techA"}]}]}}""")
  }

  "Setting two nodes twice" should "not error" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Child {
        | id: ID! @id
        | c: String! @unique
        | parents: [Parent] $listInlineDirective
        |}
        |
        |type Parent {
        | id: ID! @id
        | p: String! @unique
        | children: [Child]
        |}
      """.stripMargin
    }
    database.setup(project)

    val parentId = server
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

    val childId = server
      .query(
        """mutation {
          |  createParent(data: {
          |    p: "p2"
          |    children: {
          |      create: {c: "c1"}
          |    }
          |  }){
          |    children{id}
          |  }
          |}""",
        project
      )
      .pathAsString("data.createParent.children.[0].id")

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }

    val res = server.query(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    children: {set: {id: "$childId"}}
         |  }){
         |    children {
         |      c
         |    }
         |  }
         |}
      """,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"children":[{"c":"c1"}]}}}""")

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }

    server.query(
      s"""
         |mutation {
         |  updateParent(
         |  where:{id: "$parentId"}
         |  data:{
         |    children: {set: {id: "$childId"}}
         |  }){
         |    children {
         |      c
         |    }
         |  }
         |}
      """,
      project
    )

    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
  }

  "Setting several times" should "not error and only connect the item once" in {

    val project = SchemaDsl.fromStringV11() {
      s"""
        |type Post {
        |  id: ID! @id
        |  authors: [AUser] $listInlineDirective
        |  title: String! @unique
        |}
        |
        |type AUser {
        |  id: ID! @id
        |  name: String! @unique
        |  posts: [Post]
        |}"""
    }

    database.setup(project)

    server.query(s""" mutation {createPost(data: {title:"Title"}) {title}} """, project)
    server.query(s""" mutation {createAUser(data: {name:"Author"}) {name}} """, project)

    server.query(s""" mutation {updateAUser(where: { name: "Author"}, data:{posts:{set:{title: "Title"}}}) {name}} """, project)
    server.query(s""" mutation {updateAUser(where: { name: "Author"}, data:{posts:{set:{title: "Title"}}}) {name}} """, project)
    server.query(s""" mutation {updateAUser(where: { name: "Author"}, data:{posts:{set:{title: "Title"}}}) {name}} """, project)

    server.query("""query{aUsers{name, posts{title}}}""", project).toString should be("""{"data":{"aUsers":[{"name":"Author","posts":[{"title":"Title"}]}]}}""")
  }

  println(listInlineDirective)
}
