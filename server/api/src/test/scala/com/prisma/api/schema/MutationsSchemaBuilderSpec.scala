package com.prisma.api.schema

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class MutationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the create Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("tag", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("createTodo(data: TodoCreateInput!): Todo!")
    schema should containInputType("TodoCreateInput", fields = Vector("title: String!", "tag: String"))
  }

  "the create Mutation for a model with relations" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .oneToManyRelation("comments", "todo", comment)
        .oneToOneRelation_!("topComment", "topCommentFor", comment)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    // from Todo to Comment
    schema should containMutation("createTodo(data: TodoCreateInput!): Todo!")
    schema should containInputType(
      "TodoCreateInput",
      fields = Vector(
        "title: String!",
        "tag: String",
        "comments: CommentCreateManyWithoutTodoInput",
        "topComment: CommentCreateOneWithoutTodoInput!"
      )
    )

    schema should containInputType("CommentCreateManyWithoutTodoInput",
                                   fields = Vector(
                                     "create: [CommentCreateWithoutTodoInput!]",
                                     "connect: [CommentWhereUniqueInput!]"
                                   ))

    schema should containInputType("CommentCreateWithoutTodoInput", fields = Vector("text: String!"))

    // from Comment to Todo
    schema should containInputType("CommentCreateInput",
                                   fields = Vector(
                                     "text: String!",
                                     "todo: TodoCreateOneWithoutCommentsInput"
                                   ))

    schema should containInputType("TodoCreateOneWithoutCommentsInput",
                                   fields = Vector(
                                     "create: TodoCreateWithoutCommentsInput",
                                     "connect: TodoWhereUniqueInput"
                                   ))

    schema should containInputType("TodoCreateWithoutCommentsInput",
                                   fields = Vector(
                                     "title: String!",
                                     "tag: String"
                                   ))
  }

  "the update Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    schema should containInputType("TodoUpdateInput",
                                   fields = Vector(
                                     "title: String",
                                     "alias: String"
                                   ))

    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID",
                                     "alias: String"
                                   ))
  }

  "the update Mutation for a model with a optional backrelation" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("listUnique", _.String, isUnique = true).field("optList", _.String)
      val todo = schema.model("Todo").field_!("todoUnique", _.String, isUnique = true).field("optString", _.String)
      list.manyToManyRelation("todoes", "does not matter", todo, includeFieldB = false)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    schema should containInputType("TodoCreateInput",
                                   fields = Vector(
                                     "todoUnique: String!",
                                     "optString: String"
                                   ))

    schema should containInputType("TodoUpdateInput",
                                   fields = Vector(
                                     "todoUnique: String",
                                     "optString: String"
                                   ))

    schema should containInputType("TodoUpdateDataInput",
                                   fields = Vector(
                                     "todoUnique: String",
                                     "optString: String"
                                   ))

    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID",
                                     "todoUnique: String"
                                   ))

    schema should containInputType("TodoUpdateWithWhereUniqueNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "data: TodoUpdateDataInput!"
                                   ))

    schema should containInputType("TodoUpsertWithWhereUniqueNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "update: TodoUpdateDataInput!",
                                     "create: TodoCreateInput!"
                                   ))
  }

  "the update many Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("updateManyTodoes(data: TodoUpdateInput!, where: TodoWhereInput!): BatchPayload!")
    schema should containInputType("TodoWhereInput")
  }

  "the many update Mutation for a model" should "not be generated for an empty model" in {
    val project = SchemaDsl() { schema =>
      val model = schema.model("Todo")
      model.fields.clear()
      model.field_!("id", _.GraphQLID, isHidden = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema shouldNot containMutation("updateManyTodoes(data: TodoUpdateInput!, where: TodoWhereInput!): BatchPayload!")
    schema should containInputType("TodoWhereInput",
                                   fields = Vector(
                                     "AND: [TodoWhereInput!]",
                                     "OR: [TodoWhereInput!]"
                                   ))
  }

  "the update Mutation for a model with relations" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .oneToManyRelation("comments", "todo", comment)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    // from Todo to Comment
    schema should containMutation("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    schema should containInputType("TodoUpdateInput",
                                   fields = Vector(
                                     "title: String",
                                     "tag: String",
                                     "comments: CommentUpdateManyWithoutTodoInput"
                                   ))

    schema should containInputType(
      "CommentUpdateManyWithoutTodoInput",
      fields = Vector(
        "create: [CommentCreateWithoutTodoInput!]",
        "connect: [CommentWhereUniqueInput!]",
        "disconnect: [CommentWhereUniqueInput!]",
        "delete: [CommentWhereUniqueInput!]",
        "update: [CommentUpdateWithWhereUniqueWithoutTodoInput!]",
        "upsert: [CommentUpsertWithWhereUniqueWithoutTodoInput!]"
      )
    )

    schema should containInputType("CommentCreateWithoutTodoInput",
                                   fields = Vector(
                                     "text: String!"
                                   ))

    schema should containInputType("CommentUpdateWithWhereUniqueWithoutTodoInput",
                                   fields = Vector(
                                     "where: CommentWhereUniqueInput!",
                                     "data: CommentUpdateWithoutTodoDataInput!"
                                   ))

    schema should containInputType("CommentUpdateWithoutTodoDataInput",
                                   fields = Vector(
                                     "text: String"
                                   ))

    schema should containInputType(
      "CommentUpsertWithWhereUniqueWithoutTodoInput",
      fields = Vector(
        "where: CommentWhereUniqueInput!",
        "update: CommentUpdateWithoutTodoDataInput!",
        "create: CommentCreateWithoutTodoInput!"
      )
    )

    // from Comment to Todo
    schema should containInputType("CommentUpdateInput",
                                   fields = Vector(
                                     "text: String",
                                     "todo: TodoUpdateOneWithoutCommentsInput"
                                   ))

    schema should containInputType(
      "TodoUpdateOneWithoutCommentsInput",
      fields = Vector(
        "create: TodoCreateWithoutCommentsInput",
        "connect: TodoWhereUniqueInput",
        "disconnect: Boolean",
        "delete: Boolean",
        "update: TodoUpdateWithoutCommentsDataInput"
      )
    )

    schema should containInputType("TodoCreateWithoutCommentsInput",
                                   fields = Vector(
                                     "title: String!",
                                     "tag: String"
                                   ))

    schema should containInputType("TodoUpdateWithoutCommentsDataInput",
                                   fields = Vector(
                                     "title: String",
                                     "tag: String"
                                   ))
  }

  "the update and upsert Mutation for a model with omitted back relation" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      val todo    = schema.model("Todo").field_!("title", _.String).field("tag", _.String)
      todo.oneToManyRelation("comments", "todo", comment, includeFieldB = false)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containInputType(
      "CommentUpdateManyInput",
      fields = Vector(
        "create: [CommentCreateInput!]",
        "connect: [CommentWhereUniqueInput!]",
        "disconnect: [CommentWhereUniqueInput!]",
        "delete: [CommentWhereUniqueInput!]",
        "update: [CommentUpdateWithWhereUniqueNestedInput!]",
        "upsert: [CommentUpsertWithWhereUniqueNestedInput!]"
      )
    )
  }

  "the upsert Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("upsertTodo(where: TodoWhereUniqueInput!, create: TodoCreateInput!, update: TodoUpdateInput!): Todo!")
  }

  "the delete Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("tag", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("deleteTodo(where: TodoWhereUniqueInput!): Todo")
    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID"
                                   ))
  }

  "the delete Mutation for a model" should "be generated correctly and contain all non-list unique fields" in {
    val project = SchemaDsl() { schema =>
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .field("unique", _.Int, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("deleteTodo(where: TodoWhereUniqueInput!): Todo")
    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID",
                                     "unique: Int"
                                   ))
  }
  "the delete many Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema
        .model("Todo")
        .field_!("title", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("deleteManyTodoes(where: TodoWhereInput!): BatchPayload!")
    schema should containInputType("TodoWhereInput")
  }

  "Sample schema with relation and id only types" should "be generated correctly" in {

    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type B {
        |  id: ID! @unique
        |  rel: User
        |  c: C
        |}
        |
        |type C {
        |  id: ID! @unique
        |  b: B
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containInputType(
      "CUpdateOneWithoutBInput",
      fields = Vector(
        "connect: CWhereUniqueInput",
        "disconnect: Boolean",
        "delete: Boolean"
      )
    )

    schema.toString should include("""input CUpdateOneWithoutBInput {
                                     |  connect: CWhereUniqueInput
                                     |  disconnect: Boolean
                                     |  delete: Boolean
                                     |}""".stripMargin)
  }

  "Sample schema with relation only types" should "be generated correctly" in {

    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |}
        |
        |type B {
        |  rel: User
        |  c: C
        |}
        |
        |type C {
        |  name: String!
        |  b: B
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes =
      """
        |input BCreateInput {
        |  rel: UserCreateOneInput
        |  c: CCreateOneWithoutBInput
        |}
        |
        |input BCreateOneWithoutCInput {
        |  create: BCreateWithoutCInput
        |}
        |
        |input BCreateWithoutCInput {
        |  rel: UserCreateOneInput
        |}
        |
        |input BUpdateInput {
        |  rel: UserUpdateOneInput
        |  c: CUpdateOneWithoutBInput
        |}
        |
        |input BUpdateOneWithoutCInput {
        |  create: BCreateWithoutCInput
        |  disconnect: Boolean
        |  delete: Boolean
        |  update: BUpdateWithoutCDataInput
        |  upsert: BUpsertWithoutCInput
        |}
        |
        |input BUpdateWithoutCDataInput {
        |  rel: UserUpdateOneInput
        |}
        |
        |input BUpsertWithoutCInput {
        |  update: BUpdateWithoutCDataInput!
        |  create: BCreateWithoutCInput!
        |}
        |
        |input CCreateInput {
        |  name: String!
        |  b: BCreateOneWithoutCInput
        |}
        |
        |input CCreateOneWithoutBInput {
        |  create: CCreateWithoutBInput
        |}
        |
        |input CCreateWithoutBInput {
        |  name: String!
        |}
        |
        |input CUpdateInput {
        |  name: String
        |  b: BUpdateOneWithoutCInput
        |}
        |
        |input CUpdateOneWithoutBInput {
        |  create: CCreateWithoutBInput
        |  disconnect: Boolean
        |  delete: Boolean
        |  update: CUpdateWithoutBDataInput
        |  upsert: CUpsertWithoutBInput
        |}
        |
        |input CUpdateWithoutBDataInput {
        |  name: String
        |}
        |
        |input CUpsertWithoutBInput {
        |  update: CUpdateWithoutBDataInput!
        |  create: CCreateWithoutBInput!
        |}
        |
        |input UserCreateInput {
        |  name: String!
        |}
        |
        |input UserCreateOneInput {
        |  create: UserCreateInput
        |  connect: UserWhereUniqueInput
        |}
        |
        |input UserUpdateDataInput {
        |  name: String
        |}
        |
        |input UserUpdateInput {
        |  name: String
        |}
        |
        |input UserUpdateOneInput {
        |  create: UserCreateInput
        |  connect: UserWhereUniqueInput
        |  disconnect: Boolean
        |  delete: Boolean
        |  update: UserUpdateDataInput
        |  upsert: UserUpsertNestedInput
        |}
        |
        |input UserUpsertNestedInput {
        |  update: UserUpdateDataInput!
        |  create: UserCreateInput!
        |}
        |
        |input UserWhereUniqueInput {
        |  id: ID
        |}"""

    inputTypes.split("input").map(x => schema should include(x.stripMargin))
  }

  "Sample schema with optional back relations" should "be generated correctly" ignore {

    val project = SchemaDsl.fromString() {

      """type User{
        |   name: String! @unique
        |   friends: [OtherUser!]!  @relation(name: "UserFriends")
        |   bestFriend: OtherUser   @relation(name: "BestFriend")
        |   bestBuddy: OtherUser
        |}
        |
        |type OtherUser{
        |   name: String! @unique
        |   friendOf: [User!]! @relation(name: "UserFriends")
        |   bestFriendOf: User @relation(name: "BestFriend")
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    println(schema)

    // todo validate the full schema
  }

  "Sample schema with selfrelations and optional backrelations" should "be generated correctly" ignore {

    //todo at the moment this does not pass the schema validator although it should be valid

    val project = SchemaDsl.fromString() {

      """type User{
        |   name: String! @unique
        |   friends: [User!]!  @relation(name: "UserFriends")
        |   friendOf: [User!]! @relation(name: "UserFriends")
        |   bestFriend: User   @relation(name: "BestFriend")
        |   bestFriendOf: User @relation(name: "BestFriend")
        |   bestBuddy: User
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    // todo also generates invalid inputtypes for selfrelations

//    """input UserCreateManyWithoutFriendOfInput {
//      |  create: [UserCreateWithoutFriendsInput!]
//      |  connect: [UserWhereUniqueInput!]
//      |}"""

    // todo also generates invalid inputtypes for optional backrelations

//    """input UserCreateWithoutFriendsInput {
//      |  name: String!
//      |  bestFriend: UserCreateOneWithoutBestFriendOfInput
//      |  bestFriendOf: UserCreateOneWithoutBestFriendInput
//      |  bestBuddy: UserCreateOneWithoutBestFriendInput
//      |}"""
  }
}
