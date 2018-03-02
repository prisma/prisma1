package com.prisma.api.schema

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class InputTypesSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

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

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input BCreateInput {
                       |  rel: UserCreateOneInput
                       |  c: CCreateOneWithoutBInput
                       |}
                       |
                       |input BCreateOneWithoutCInput {
                       |  create: BCreateWithoutCInput
                       |  connect: BWhereUniqueInput
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
                       |  connect: BWhereUniqueInput
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
                       |input BWhereUniqueInput {
                       |  id: ID
                       |}
                       |
                       |input CCreateInput {
                       |  b: BCreateOneWithoutCInput
                       |}
                       |
                       |input CCreateOneWithoutBInput {
                       |  connect: CWhereUniqueInput
                       |}
                       |
                       |input CUpdateInput {
                       |  b: BUpdateOneWithoutCInput
                       |}
                       |
                       |input CUpdateOneWithoutBInput {
                       |  connect: CWhereUniqueInput
                       |  disconnect: Boolean
                       |  delete: Boolean
                       |}
                       |
                       |input CWhereUniqueInput {
                       |  id: ID
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

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))

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

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Sample schema with selfrelations" should "generate the correct input types" in {

    val project = SchemaDsl.fromString() {

      """type User{
        |   name: String! @unique
        |   friend: User! @relation(name: "UserFriends")
        |   friendOf: User! @relation(name: "UserFriends")
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input UserCreateInput {
                       |  name: String!
                       |  friend: UserCreateOneWithoutFriendOfInput!
                       |  friendOf: UserCreateOneWithoutFriendInput!
                       |}
                       |
                       |input UserCreateOneWithoutFriendInput {
                       |  create: UserCreateWithoutFriendInput
                       |  connect: UserWhereUniqueInput
                       |}
                       |
                       |input UserCreateOneWithoutFriendOfInput {
                       |  create: UserCreateWithoutFriendOfInput
                       |  connect: UserWhereUniqueInput
                       |}
                       |
                       |input UserCreateWithoutFriendInput {
                       |  name: String!
                       |}
                       |
                       |input UserCreateWithoutFriendOfInput {
                       |  name: String!
                       |}
                       |
                       |input UserUpdateInput {
                       |  name: String
                       |  friend: UserUpdateOneWithoutFriendOfInput
                       |  friendOf: UserUpdateOneWithoutFriendInput
                       |}
                       |
                       |input UserUpdateOneWithoutFriendInput {
                       |  create: UserCreateWithoutFriendInput
                       |  connect: UserWhereUniqueInput
                       |  disconnect: Boolean
                       |  delete: Boolean
                       |  update: UserUpdateWithoutFriendDataInput
                       |  upsert: UserUpsertWithoutFriendInput
                       |}
                       |
                       |input UserUpdateOneWithoutFriendOfInput {
                       |  create: UserCreateWithoutFriendOfInput
                       |  connect: UserWhereUniqueInput
                       |  disconnect: Boolean
                       |  delete: Boolean
                       |  update: UserUpdateWithoutFriendOfDataInput
                       |  upsert: UserUpsertWithoutFriendOfInput
                       |}
                       |
                       |input UserUpdateWithoutFriendDataInput {
                       |  name: String
                       |}
                       |
                       |input UserUpdateWithoutFriendOfDataInput {
                       |  name: String
                       |}
                       |
                       |input UserUpsertWithoutFriendInput {
                       |  update: UserUpdateWithoutFriendDataInput!
                       |  create: UserCreateWithoutFriendInput!
                       |}
                       |
                       |input UserUpsertWithoutFriendOfInput {
                       |  update: UserUpdateWithoutFriendOfDataInput!
                       |  create: UserCreateWithoutFriendOfInput!
                       |}
                       |
                       |input UserWhereUniqueInput {
                       |  name: String
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Sample schema with selfrelations and optional backrelations" should "be generated correctly" ignore {

    //todo at the moment this does not pass the schema validator although it should probably be valid

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
}
