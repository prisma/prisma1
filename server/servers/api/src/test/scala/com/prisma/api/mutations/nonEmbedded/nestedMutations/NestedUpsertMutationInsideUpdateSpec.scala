package com.prisma.api.mutations.nonEmbedded.nestedMutations

import java.util.UUID

import com.prisma.{IgnoreMongo, IgnoreMySql}
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class NestedUpsertMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  "a PM to C1!  relation with a child already in a relation" should "work with create" in {
    val project = SchemaDsl.fromString() { schemaPMToC1req }
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
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |      childrenOpt: {
         |        upsert: {
         |          where: {c: "DOES NOT EXIST"}
         |          update: {c: "DOES NOT MATTER"}
         |          create :{c: "c2"}
         |        }
         |      }
         |   }
         |){
         |  childrenOpt {
         |    c
         |  }
         |}
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"}]}}}""")

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)
    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(3) }
  }

  "a PM to C1!  relation with a child already in a relation" should "work with update" in {
    val project = SchemaDsl.fromString() { schemaPMToC1req }
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
         |  updateParent(
         |    where: {p: "p1"}
         |    data:{
         |    childrenOpt: {upsert: {
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"updated C"}]}}}""")

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(1)
    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(1) }
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(2) }
  }

  "a PM to C1  relation with the parent already in a relation" should "work through a nested mutation by unique for create" in {
    val project = SchemaDsl.fromString() { schemaPMToC1opt }
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
         |    childrenOpt: {upsert: [{
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "new C"}
         |    }]}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"},{"c":"new C"}]}}}""")

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(3)
    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(3) }
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(4) }
  }

  "a PM to C1  relation with the parent already in a relation" should "work through a nested mutation by unique for update" in {
    val project = SchemaDsl.fromString() { schemaPMToC1opt }
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
         |    childrenOpt: {upsert: [{
         |    where: {c: "c1"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }]}
         |  }){
         |    childrenOpt {
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"updated C"},{"c":"c2"}]}}}""")

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)
    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(3) }
  }

  "a PM to CM  relation with the children already in a relation" should "work through a nested mutation by unique for update" in {
    val project = SchemaDsl.fromString() { schemaPMToCM }
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
         |    childrenOpt: {upsert: [{
         |    where: {c: "c2"}
         |    update: {c: "updated C"}
         |    create :{c: "DOES NOT MATTER"}
         |    }]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"updated C"}]}}}""")

    server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"}]},{"c":"updated C","parentsOpt":[{"p":"p1"}]}]}}""")

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(2)
    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(2) }
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(3) }
  }

  "a PM to CM  relation with the children already in a relation" should "work through a nested mutation by unique for create" in {
    val project = SchemaDsl.fromString() { schemaPMToCM }
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
         |    childrenOpt: {upsert: [{
         |    where: {c: "DOES NOT EXIST"}
         |    update: {c: "DOES NOT MATTER"}
         |    create :{c: "updated C"}
         |    }]}
         |  }){
         |    childrenOpt{
         |      c
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    res.toString should be("""{"data":{"updateParent":{"childrenOpt":[{"c":"c1"},{"c":"c2"},{"c":"updated C"}]}}}""")

    server.query(s"""query{children{c, parentsOpt{p}}}""", project).toString should be(
      """{"data":{"children":[{"c":"c1","parentsOpt":[{"p":"p1"}]},{"c":"c2","parentsOpt":[{"p":"p1"}]},{"c":"updated C","parentsOpt":[{"p":"p1"}]}]}}""")

    dataResolver(project).countByTable(project.schema.getModelByName_!("Parent").dbName).await should be(1)
    dataResolver(project).countByTable(project.schema.getModelByName_!("Child").dbName).await should be(3)
    ifConnectorIsActive { dataResolver(project).countByTable("_ChildToParent").await should be(3) }
    ifConnectorIsActive { dataResolver(project).countByTable("_RelayId").await should be(4) }
  }

  "a one to many relation" should "be upsertable by id through a nested mutation" in {
    val schema = """type Comment{
                            id: ID! @unique
                            text: String
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @unique
                            comments: [Comment]
                        }"""

    val project = SchemaDsl.fromString() { schema }
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

    val result = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "$comment1Id"}, update: {text: "update comment1"}, create: {text: "irrelevant"}},
         |          {where: {id: "5beea4aa6183dd734b2dbd9b"}, update: {text: "irrelevant"}, create: {text: "new comment3"}},
         |        ]
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

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """comment2""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[2].text").toString, """new comment3""")
  }

  "a one to many relation" should "only update nodes that are connected" in {
    val schema = """type Comment{
                            id: ID! @unique
                            text: String
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @unique
                            comments: [Comment]
                        }"""

    val project = SchemaDsl.fromString() { schema }
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
    val todoId     = createResult.pathAsString("data.createTodo.id")
    val comment1Id = createResult.pathAsString("data.createTodo.comments.[0].id")

    val commentResult = server.query(
      """mutation {
        |  createComment(
        |    data: {
        |      text: "comment2"
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )
    val comment2Id = commentResult.pathAsString("data.createComment.id")

    val result = server.query(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "$comment1Id"}, update: {text: "update comment1"}, create: {text: "irrelevant"}},
         |          {where: {id: "$comment2Id"}, update: {text: "irrelevant"}, create: {text: "new comment3"}},
         |        ]
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

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """new comment3""")
  }

  "a one to many relation" should "generate helpful error messages" in {
    val schema = """type Comment{
                            id: ID! @unique
                            text: String
                            uniqueComment: String! @unique
                            todo: Todo
                        }

                        type Todo{
                            id: ID! @unique
                            uniqueTodo: String! @unique
                            comments: [Comment]
                        }"""

    val project = SchemaDsl.fromString() { schema }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createTodo(
        |    data: {
        |      uniqueTodo: "todo"
        |      comments: {
        |        create: [{text: "comment1", uniqueComment: "comments"}]
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

    server.queryThatMustFail(
      s"""mutation {
         |  updateTodo(
         |    where: {
         |      id: "$todoId"
         |    }
         |    data:{
         |      comments: {
         |        upsert: [
         |          {where: {id: "5beea4aa6183dd734b2dbd9b"}, update: {text: "update comment1"}, create: {text: "irrelevant", uniqueComment: "comments"}},
         |        ]
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
      errorCode = 3010,
      errorContains = "A unique constraint would be violated on Comment. Details: Field name = uniqueComment"
    )
  }

  "a deeply nested mutation" should "execute all levels of the mutation" in {

    val schema = """     type List{
                            id: ID! @unique
                            name: String!
                            todos: [Todo]
                        }

                        type Todo{
                            id: ID! @unique
                            title: String!
                            list: List
                            tags: [Tag]
                        }

                        type Tag{
                            id: ID! @unique
                            name: String!
                            todo: Todo
                        }"""

    val project = SchemaDsl.fromString() { schema }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tags: {
        |            create: [
        |              {name: "the tag"}
        |            ]
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    id
        |    todos {
        |      id
        |      tags {
        |        id
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val createResult = server.query(createMutation, project)
    val listId       = createResult.pathAsString("data.createList.id")
    val todoId       = createResult.pathAsString("data.createList.todos.[0].id")
    val tagId        = createResult.pathAsString("data.createList.todos.[0].tags.[0].id")

    val updateMutation =
      s"""
         |mutation  {
         |  updateList(
         |    where: {
         |      id: "$listId"
         |    }
         |    data: {
         |      todos: {
         |        upsert: [
         |          {
         |            where: { id: "$todoId" }
         |            create: { title: "irrelevant" }
         |            update: {
         |              tags: {
         |                upsert: [
         |                  {
         |                    where: { id: "$tagId" }
         |                    update: { name: "updated tag" }
         |                    create: { name: "irrelevant" }
         |                  },
         |                  {
         |                    where: { id: "5beea4aa6183dd734b2dbd9b" }
         |                    update: { name: "irrelevant" }
         |                    create: { name: "new tag" }
         |                  },
         |                ]
         |              }
         |            }
         |          }
         |        ]
         |      }
         |    }
         |  ) {
         |    name
         |    todos {
         |      title
         |      tags {
         |        name
         |      }
         |    }
         |  }
         |}
      """.stripMargin

    val result = server.query(updateMutation, project)
    result.pathAsString("data.updateList.todos.[0].tags.[0].name") should equal("updated tag")
    result.pathAsString("data.updateList.todos.[0].tags.[1].name") should equal("new tag")
  }

  "a deeply nested mutation with upsert" should "work on miss on id" in {
    val schema = """     type List{
                            id: ID! @unique
                            name: String!
                            todos: [Todo]
                        }

                        type Todo{
                            id: ID! @unique
                            title: String!
                            list: List
                            tags: [Tag]
                        }

                        type Tag{
                            id: ID! @unique
                            name: String!
                            todo: Todo
                        }"""

    val project = SchemaDsl.fromString() { schema }
    database.setup(project)

    val createMutation =
      """
        |mutation  {
        |  createList(data: {
        |    name: "the list",
        |    todos: {
        |      create: [
        |        {
        |          title: "the todo"
        |          tags: {
        |            create: [
        |              {name: "the tag"}
        |            ]
        |          }
        |        }
        |      ]
        |    }
        |  }) {
        |    id
        |    todos {
        |      id
        |      tags {
        |        id
        |      }
        |    }
        |  }
        |}
      """.stripMargin

    val createResult = server.query(createMutation, project)
    val listId       = createResult.pathAsString("data.createList.id")
    val todoId       = createResult.pathAsString("data.createList.todos.[0].id")
    val tagId        = createResult.pathAsString("data.createList.todos.[0].tags.[0].id")

    val updateMutation =
      s"""
         |mutation  {
         |  updateList(
         |    where: {
         |      id: "$listId"
         |    }
         |    data: {
         |      todos: {
         |        upsert: [
         |          {
         |            where: { id: "5beea4aa6183dd734b2dbd9b" }
         |            create: { title: "new todo" tags: { create: [ {name: "the tag"}]}}
         |            update: { title: "updated todo"}
         |          }
         |        ]
         |      }
         |    }
         |  ) {
         |    name
         |    todos {
         |      title
         |      tags {
         |        name
         |      }
         |    }
         |  }
         |}
      """.stripMargin

    val result = server.query(updateMutation, project)
    result.pathAsString("data.updateList.todos.[0].tags.[0].name") should equal("the tag")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path for update case" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top]
                                             |  bottoms: [Bottom]
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
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
         |                      bottoms: {upsert: [{ where: {nameBottom: "the bottom"},
         |                                           create:  {nameBottom: "Should not matter"}
         |                                           update:  {nameBottom: "updated bottom"}
         |                      }]
         |              }
         |              }}]
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
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"updated bottom"},{"nameBottom":"the second bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"updated bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path for create case" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top]
                                             |  bottoms: [Bottom]
                                             |}
                                             |
                                             |type Bottom {
                                             |  id: ID! @unique
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
         |                      bottoms: {upsert: [{ where: {nameBottom: "does not exist"},
         |                                           create:  {nameBottom: "created bottom"}
         |                                           update:  {nameBottom: "should not matter"}
         |                      }]
         |              }
         |              }}]
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
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"created bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"},{"nameBottom":"created bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path for update case with no backrelations" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  bottoms: [Bottom]
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
         |                      bottoms: {upsert: [{ where: {nameBottom: "the bottom"},
         |                                           create:  {nameBottom: "Should not matter"}
         |                                           update:  {nameBottom: "updated bottom"}
         |                      }]
         |              }
         |              }}]
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
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"updated bottom"},{"nameBottom":"the second bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"updated bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path for create case with no backrelations" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  bottoms: [Bottom]
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
         |                      bottoms: {upsert: [{ where: {nameBottom: "does not exist"},
         |                                           create:  {nameBottom: "created bottom"}
         |                                           update:  {nameBottom: "should not matter"}
         |                      }]
         |              }
         |              }}]
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
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"created bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"the bottom"},{"nameBottom":"the second bottom"},{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"},{"nameBottom":"created bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path for update" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top]
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
         |                      bottom: {upsert: {create: {nameBottom: "should not matter"},
         |                                        update: {nameBottom: "updated bottom"}}}
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
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom"}},{"nameMiddle":"the second middle","bottom":{"nameBottom":"the second bottom"}}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"updated bottom"},{"nameBottom":"the second bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path for create" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle {
                                             |  id: ID! @unique
                                             |  nameMiddle: String! @unique
                                             |  tops: [Top]
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
         |                      bottom: {upsert: {create: {nameBottom: "created bottom"},
         |                                        update: {nameBottom: "should not matter"}}}
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
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottom":{"nameBottom":"created bottom"}},{"nameMiddle":"the second middle","bottom":{"nameBottom":"the second bottom"}}]}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be(
      """{"data":{"bottoms":[{"nameBottom":"the second bottom"},{"nameBottom":"created bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path  and back relations are missing and node edges follow model edges for update" in {
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
                                             |  below: [Below]
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
         |                  below: { upsert: {
         |                    where: {nameBelow: "below"}
         |                    create:{nameBelow: "should not matter"}
         |                    update:{nameBelow: "updated below"}
         |                  }
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
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom","below":[{"nameBelow":"updated below"},{"nameBelow":"second below"}]}}}}}""")

    server.query("query{belows{nameBelow}}", project).toString should be("""{"data":{"belows":[{"nameBelow":"updated below"},{"nameBelow":"second below"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path  and back relations are missing and node edges follow model edges for create" in {
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
                                             |  below: [Below]
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
         |                  below: { upsert: {
         |                    where: {nameBelow: "Does not exist"}
         |                    create:{nameBelow: "created below"}
         |                    update:{nameBelow: "should not matter"}
         |                  }
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
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom","below":[{"nameBelow":"below"},{"nameBelow":"second below"},{"nameBelow":"created below"}]}}}}}""")

    server.query("query{belows{nameBelow}}", project).toString should be(
      """{"data":{"belows":[{"nameBelow":"below"},{"nameBelow":"second below"},{"nameBelow":"created below"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path for update" in {
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
         |              bottom: {upsert: {create: {nameBottom: "should not matter"},
         |                                update: {nameBottom: "updated bottom"}
         |                       }
         |              }
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

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom"}}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"updated bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path for create" in {
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
        |      create:{nameMiddle: "the middle"}
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
         |              bottom: {upsert: {create: {nameBottom: "created bottom"},
         |                                update: {nameBottom: "should not matter"}
         |                       }
         |              }
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

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"created bottom"}}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"created bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path for update when there are no backrelations" in {
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
         |              bottom: {upsert: {create: {nameBottom: "should not matter"},
         |                                update: {nameBottom: "updated bottom"}
         |                       }
         |              }
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

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom"}}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"updated bottom"}]}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path for create when there are no backrelations" in {
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
        |      create:{nameMiddle: "the middle"}
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
         |              bottom: {upsert: {create: {nameBottom: "created bottom"},
         |                                update: {nameBottom: "should not matter"}
         |                       }
         |              }
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

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"created bottom"}}}}}""")

    server.query("query{bottoms{nameBottom}}", project).toString should be("""{"data":{"bottoms":[{"nameBottom":"created bottom"}]}}""")
  }

  "a nested upsert for a type with an id field of type uuid" should "work" taggedAs (IgnoreMySql, IgnoreMongo) in {
    val project = SchemaDsl.fromString() {
      s"""
         |type List {
         |  id: ID! @unique
         |  todos: [Todo]
         |}
         |
         |type Todo {
         |  id: UUID! @unique
         |  title: String!
         |}
       """.stripMargin
    }
    database.setup(project)

    val listId = server
      .query("""
        |mutation {
        |  createList(data: {}) {
        |    id
        |  }
        |}
      """.stripMargin,
             project)
      .pathAsString("data.createList.id")

    val result = server.query(
      s"""
        |mutation {
        |  updateList(
        |  where: {id: "$listId"}
        |  data: {
        |    todos: {
        |      upsert: [
        |        {
        |          where: { id: "00000000-0000-0000-0000-000000000000" }
        |          create: { title: "the todo" }
        |          update: { title: "the updated title" }
        |        }
        |      ]
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

    result.pathAsString("data.updateList.todos.[0].title") should equal("the todo")
    val theUuid = result.pathAsString("data.updateList.todos.[0].id")
    UUID.fromString(theUuid) // should now blow up
  }
}
