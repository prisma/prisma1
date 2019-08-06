package writes.nonEmbedded.nestedMutations

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NestedUpdateMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase with SchemaBaseV11 {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)

  "a one to many relation" should "be updateable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo {
        | id: ID! @id
        | comments: [Comment] $listInlineDirective
        |}
        |
        |type Comment {
        | id: ID! @id
        | text: String
        | todo: Todo!
        |}
      """.stripMargin
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
         |        update: [
         |          {where: {id: "$comment1Id"}, data: {text: "update comment1"}},
         |          {where: {id: "$comment2Id"}, data: {text: "update comment2"}},
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
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """update comment2""")
  }

  "a one to many relation" should "be updateable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo {
        | id: ID! @id
        | comments: [Comment] $listInlineDirective
        |}
        |
        |type Comment {
        | id: ID! @id
        | alias: String! @unique
        | text: String
        | todo: Todo!
        |}
      """.stripMargin
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
         |        update: [
         |          {where: {alias: "alias1"}, data: {text: "update comment1"}},
         |          {where: {alias: "alias2"}, data: {text: "update comment2"}}
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
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """update comment2""")
  }

  "a many to many relation with an optional backrelation" should "be updateable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type List {
        | id: ID! @id
        | listUnique: String! @unique
        | todoes: [Todo] $listInlineDirective
        |}
        |
        |type Todo {
        | id: ID! @id
        | todoUnique: String! @unique
        |}
      """.stripMargin
    }
    database.setup(project)

    server.query(
      """mutation {
        |  createList(
        |    data: {
        |      listUnique : "list",
        |      todoes: {
        |        create: [{todoUnique: "todo"}]
        |      }
        |    }
        |  ){
        |    listUnique
        |    todoes { todoUnique }
        |  }
        |}""".stripMargin,
      project
    )
    val result = server.query(
      s"""mutation {
         |  updateList(
         |    where: {
         |      listUnique: "list"
         |    }
         |    data:{
         |      todoes: {
         |        update: [{where: {todoUnique: "todo"}, data: {todoUnique: "new todo"}}]
         |      }
         |    }
         |  ){
         |    listUnique
         |    todoes{
         |      todoUnique
         |    }
         |  }
         |}
      """.stripMargin,
      project
    )

    mustBeEqual(result.pathAsString("data.updateList.todoes.[0].todoUnique").toString, """new todo""")
  }

  "a many to one relation" should "be updateable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo {
        | id: ID! @id
        | title: String
        | comments: [Comment] $listInlineDirective
        |}
        |
        |type Comment {
        | id: ID! @id
        | text: String!
        | todo: Todo!
        |}
      """.stripMargin
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
         |        update: {title: "updated title"}
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
    mustBeEqual(result.pathAsJsValue("data.updateComment.todo").toString, """{"title":"updated title"}""")
  }

  "a one to one relation" should "be updateable by id through a nested mutation" in {
    val project = SchemaDsl.fromStringV11() {
      """type Todo {
        | id: ID! @id
        | title: String!
        | note: Note @relation(link: INLINE)
        |}
        |
        |type Note {
        | id: ID! @id
        | text: String
        | todo: Todo
        |}
      """.stripMargin
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
         |        update: { title: "updated title" }
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
    mustBeEqual(result.pathAsJsValue("data.updateNote.todo").toString, """{"title":"updated title"}""")
  }

  //Transactionality
  "TRANSACTIONAL: a many to many relation" should "fail gracefully on wrong where and assign error correctly and not execute partially" taggedAs (IgnoreMongo) in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo {
        | id: ID! @id
        | title: String!
        | notes: [Note] $listInlineDirective
        |}
        |
        |type Note {
        | id: ID! @id
        | text: String
        | todoes: [Todo]
        |}
      """.stripMargin
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Some Text"
        |      todoes: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){
        |    id
        |    todoes { id }
        |  }
        |}""".stripMargin,
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")
    val todoId = createResult.pathAsString("data.createNote.todoes.[0].id")

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      text: "Some Changed Text"
         |      todoes: {
         |        update: {
         |          where: {id: "DOES NOT EXIST"},
         |          data:{title: "updated title"}
         |        }
         |      }
         |    }
         |  ){
         |    text
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Todo with value DOES NOT EXIST for id found."
    )

    server.query(s"""query{note(where:{id: "$noteId"}){text}}""", project, dataContains = """{"note":{"text":"Some Text"}}""")
    server.query(s"""query{todo(where:{id: "$todoId"}){title}}""", project, dataContains = """{"todo":{"title":"the title"}}""")
  }

  "NON-TRANSACTIONAL: a many to many relation" should "fail gracefully on wrong where and assign error correctly and not execute partially" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Todo {
        | id: ID! @id
        | title: String!
        | notes: [Note] $listInlineDirective
        |}
        |
        |type Note {
        | id: ID! @id
        | text: String
        | todoes: [Todo]
        |}
      """.stripMargin
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Some Text"
        |      todoes: {
        |        create: { title: "the title" }
        |      }
        |    }
        |  ){
        |    id
        |    todoes { id }
        |  }
        |}""".stripMargin,
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")
    val todoId = createResult.pathAsString("data.createNote.todoes.[0].id")

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      id: "$noteId"
         |    }
         |    data: {
         |      text: "Some Changed Text"
         |      todoes: {
         |        update: {
         |          where: {id: "5beea4aa6183dd734b2dbd9b"},
         |          data:{title: "updated title"}
         |        }
         |      }
         |    }
         |  ){
         |    text
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Todo with value 5beea4aa6183dd734b2dbd9b for id found."
    )
  }

  "a many to many relation" should "handle null in unique fields" in {
    val project = SchemaDsl.fromStringV11() {
      s"""type Note {
        | id: ID! @id
        | text: String @unique
        | todos: [Todo] $listInlineDirective
        |}
        |
        |type Todo {
        | id: ID! @id
        | title: String! @unique
        | unique: String @unique
        | notes: [Note]
        |}
      """.stripMargin
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Some Text"
        |      todos:
        |      {
        |       create: [{ title: "the title", unique: "test"},{ title: "the other title"}]
        |      }
        |    }
        |  ){
        |    id
        |    todos { id }
        |  }
        |}""".stripMargin,
      project
    )

    val result = server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: {
         |      text: "Some Text"
         |    }
         |    data: {
         |      text: "Some Changed Text"
         |      todos: {
         |        update: {
         |          where: {unique: null},
         |          data:{title: "updated title"}
         |        }
         |      }
         |    }
         |  ){
         |    text
         |    todos {
         |      title
         |    }
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3040,
      errorContains = "You provided an invalid argument for the where selector on Todo."
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
         |                      bottoms: {update: [{ where: {nameBottom: "the bottom"},
         |                                           data:  {nameBottom: "updated bottom"}
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
         |                      bottoms: {update: [{ where: {nameBottom: "the bottom"},
         |                                           data:  {nameBottom: "updated bottom"}
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
         |                      bottom: {update: {nameBottom: "updated bottom"}}
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
         |                  below: { update: {
         |                    where: {nameBelow: "below"}
         |                    data:{nameBelow: "updated below"}
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
  }

  "a deeply nested mutation" should "fail if there are model and node edges on the path and back relations are missing and node edges follow model edges but the path is interrupted" in {
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

    val createMutation2 =
      """
        |mutation a {
        |  createTop(data: {
        |    nameTop: "the second top",
        |    middle: {
        |      create:
        |        {
        |          nameMiddle: "the second middle"
        |          bottom: {
        |            create: { nameBottom: "the second bottom"
        |            below: {
        |            create: [{ nameBelow: "other below"}, { nameBelow: "second other below"}]}
        |        }}}
        |        }
        |  }) {id}
        |}
      """.stripMargin

    server.query(createMutation2, project)

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
         |                  below: { update: {
         |                    where: {nameBelow: "other below"}
         |                    data:{nameBelow: "updated below"}
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

    server.queryThatMustFail(
      updateMutation,
      project,
      errorCode = 3041,
      errorContains =
        """The relation BelowToBottom has no node for the model Bottom connected to a Node for the model Below with the value 'other below' for the field 'nameBelow' on your mutation path."""
    )
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
         |              bottom: {update: {nameBottom: "updated bottom"}}
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
         |              bottom: {update: {nameBottom: "updated bottom"}}
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
  }

  "a deeply nested mutation" should "fail if there are only model edges on the path but there is no connected item to update at the end" in {
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
        |      create:{ nameMiddle: "the middle"}
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
         |              bottom: {update: {nameBottom: "updated bottom"}}
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

    server.queryThatMustFail(
      updateMutation,
      project,
      errorCode = 3041,
      errorContains = """The relation BottomToMiddle has no node for the model Middle connected to a Node for the model Bottom on your mutation path."""
    )

  }
}
