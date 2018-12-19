package com.prisma.api.mutations.embedded.nestedMutations.nonEmbeddedToEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedNestedUpdateMutationInsideUpdateSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(EmbeddedTypesCapability)

  "Several many relations" should "be updateable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromString() {
      """type Todo {
        | id: ID! @unique
        | comments: [Comment]
        |}
        |
        |type Comment @embedded {
        | alias: String! @unique
        | text: String
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
        |  }
        |}""",
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
      """,
      project
    )

    mustBeEqual(result.pathAsString("data.updateTodo.comments.[0].text").toString, """update comment1""")
    mustBeEqual(result.pathAsString("data.updateTodo.comments.[1].text").toString, """update comment2""")
  }

  "A many relation" should "be updateable by any unique argument through a nested mutation" in {
    val project = SchemaDsl.fromString() {
      """type List {
        | id: ID! @unique
        | listUnique: String! @unique
        | todoes: [Todo]
        |}
        |
        |type Todo @embedded{
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

  "A to one relation" should "be updateable by id through a nested mutation" in {
    val project = SchemaDsl.fromString() {
      """type Todo @embedded {
        | title: String!
        |}
        |
        |type Note  {
        | id: ID! @unique
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
        |  }
        |}""",
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")

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
      """,
      project
    )
    mustBeEqual(result.pathAsJsValue("data.updateNote.todo").toString, """{"title":"updated title"}""")
  }

  "a many to many relation" should "fail gracefully on wrong where and assign error correctly and not execute partially" in {
    val project = SchemaDsl.fromString() {
      """type Todo @embedded {
        | title: String!
        | t: String! @unique
        |}
        |
        |type Note {
        | id: ID! @unique
        | text: String
        | todoes: [Todo]
        |}
      """
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createNote(
        |    data: {
        |      text: "Some Text"
        |      todoes: {
        |        create: { title: "the title", t: "Unique" }
        |      }
        |    }
        |  ){
        |    id
        |  }
        |}""".stripMargin,
      project
    )
    val noteId = createResult.pathAsString("data.createNote.id")

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
         |          where: {t: "DOES NOT EXIST"},
         |          data:{title: "updated title"}
         |        }
         |      }
         |    }
         |  ){
         |    text
         |  }
         |}
      """,
      project,
      errorCode = 3041,
      errorContains =
        "The relation NoteToTodo has no node for the model Note connected to a Node for the model Todo with the value 'DOES NOT EXIST' for the field 't' on your mutation path."
    )

    server.query(s"""query{note(where:{id: "$noteId"}){text}}""", project, dataContains = """{"note":{"text":"Some Text"}}""")
  }

  "a many to many relation" should "handle null in unique fields" in {
    val project = SchemaDsl.fromString() {
      """type Note {
        | id: ID! @unique
        | text: String @unique
        | todos: [Todo]
        |}
        |
        |type Todo @embedded{
        | title: String! @unique
        | unique: String @unique
        |}
      """
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
        |    todos { title }
        |  }
        |}""",
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
      """,
      project,
      errorCode = 3040,
      errorContains = "You provided an invalid argument for the where selector on Todo."
    )
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottoms: [Bottom]
                                             |}
                                             |
                                             |type Bottom @embedded{
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
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottoms":[{"nameBottom":"updated bottom"},{"nameBottom":"the second bottom"}]},{"nameMiddle":"the second middle","bottoms":[{"nameBottom":"the third bottom"},{"nameBottom":"the fourth bottom"}]}]}}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only node edges on the path and there are no backrelations" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottoms: [Bottom]
                                             |}
                                             |
                                             |type Bottom @embedded{
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
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middles: [Middle]
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom @embedded{
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
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middles":[{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom"}},{"nameMiddle":"the second middle","bottom":{"nameBottom":"the second bottom"}}]}}}""")
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are model and node edges on the path  and back relations are missing and node edges follow model edges" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom @embedded{
                                             |  nameBottom: String! @unique
                                             |  below: [Below]
                                             |}
                                             |
                                             |type Below @embedded{
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
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom @embedded{
                                             |  nameBottom: String! @unique
                                             |  below: [Below]
                                             |}
                                             |
                                             |type Below @embedded{
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
      """

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
      """

    server.queryThatMustFail(
      updateMutation,
      project,
      errorCode = 3041,
      errorContains =
        """The relation BelowToBottom has no node for the model Bottom connected to a Node for the model Below with the value 'other below' for the field 'nameBelow' on your mutation path."""
    )
  }

  "a deeply nested mutation" should "execute all levels of the mutation if there are only model edges on the path" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom @embedded{
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
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle @embedded{
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom @embedded{
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
      """

    val result = server.query(updateMutation, project)

    result.toString should be(
      """{"data":{"updateTop":{"nameTop":"updated top","middle":{"nameMiddle":"updated middle","bottom":{"nameBottom":"updated bottom"}}}}}""")
  }

  "a deeply nested mutation" should "fail if there are only model edges on the path but there is no connected item to update at the end" in {
    val project = SchemaDsl.fromString() { """type Top {
                                             |  id: ID! @unique
                                             |  nameTop: String! @unique
                                             |  middle: Middle
                                             |}
                                             |
                                             |type Middle @embedded {
                                             |  nameMiddle: String! @unique
                                             |  bottom: Bottom
                                             |}
                                             |
                                             |type Bottom @embedded{
                                             |  nameBottom: String! @unique
                                             |}""" }
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
      """

    server.queryThatMustFail(
      updateMutation,
      project,
      errorCode = 3041,
      errorContains = """The relation BottomToMiddle has no node for the model Middle connected to a Node for the model Bottom on your mutation path."""
    )
  }

  "Updating toOne relations" should "work" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |   id: ID! @unique
        |   unique: Int! @unique
        |   name: String!
        |   middle: Middle
        |}
        |
        |type Middle @embedded{
        |   unique: Int! @unique
        |   name: String!
        |}"""
    }

    database.setup(project)

    val res = server.query(
      s"""mutation {
         |   createTop(data: {
         |   unique: 1,
         |   name: "Top",
         |   middle: {create:{
         |      unique: 11,
         |      name: "Middle"
         |   }
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique
         |  }
         |}}""".stripMargin,
      project
    )

    res.toString should be("""{"data":{"createTop":{"unique":1,"middle":{"unique":11}}}}""")

    val res2 = server.query(
      s"""mutation {
         |   updateTop(
         |   where:{unique: 1}
         |   data: {
         |      name: "Top2",
         |      middle: {update:{
         |          name: "MiddleNew"
         |      }
         |   }
         |}){
         |  unique,
         |  middle{
         |    unique
         |    name
         |  }
         |}}""".stripMargin,
      project
    )

    res2.toString should be("""{"data":{"updateTop":{"unique":1,"middle":{"unique":11,"name":"MiddleNew"}}}}""")
  }
}
