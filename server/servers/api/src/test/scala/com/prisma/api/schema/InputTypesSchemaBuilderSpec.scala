package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, SupportsExistingDatabasesCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class InputTypesSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder
  // a lot of the schemas omit the id field which is required for passive connectors
  override def doNotRunForCapabilities = Set(SupportsExistingDatabasesCapability, EmbeddedTypesCapability)

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
                       |  id: ID
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
                       |  id: ID
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
                       |  id: ID
                       |  b: BCreateOneWithoutCInput
                       |}
                       |
                       |input CCreateWithoutBInput {
                       |  id: ID
                       |}
                       |
                       |input CCreateOneWithoutBInput {
                       |  create: CCreateWithoutBInput
                       |  connect: CWhereUniqueInput
                       |}
                       |
                       |input CUpdateInput {
                       |  b: BUpdateOneWithoutCInput
                       |}
                       |
                       |input CUpdateOneWithoutBInput {
                       |  create: CCreateWithoutBInput
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
                       |  id: ID
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
        |  name: String! @unique
        |  b: B
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes =
      """
        |input BCreateInput {
        |  id: ID
        |  rel: UserCreateOneInput
        |  c: CCreateOneWithoutBInput
        |}
        |
        |input BCreateOneWithoutCInput {
        |  create: BCreateWithoutCInput
        |}
        |
        |input BCreateWithoutCInput {
        |  id: ID
        |  rel: UserCreateOneInput
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
        |  id: ID
        |  b: BCreateOneWithoutCInput
        |}
        |
        |input CCreateOneWithoutBInput {
        |  create: CCreateWithoutBInput
        |  connect: CWhereUniqueInput
        |}
        |
        |input CCreateWithoutBInput {
        |  name: String!
        |  id: ID
        |}
        |
        |input CUpdateInput {
        |  name: String
        |  b: BUpdateOneWithoutCInput
        |}
        |
        |input CUpdateManyMutationInput {
        |  name: String
        |}
        |
        |input UserCreateInput {
        |  id: ID
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
        |input UserUpdateManyMutationInput {
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
                       |  id: ID
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
                       |  id: ID
                       |  friendOf: UserCreateOneWithoutFriendInput!
                       |}
                       |
                       |input UserCreateWithoutFriendOfInput {
                       |  name: String!
                       |  id: ID
                       |  friend: UserCreateOneWithoutFriendOfInput!
                       |}
                       |
                       |input UserUpdateInput {
                       |  name: String
                       |  friend: UserUpdateOneRequiredWithoutFriendOfInput
                       |  friendOf: UserUpdateOneRequiredWithoutFriendInput
                       |}
                       |
                       |input UserUpdateOneRequiredWithoutFriendInput {
                       |  create: UserCreateWithoutFriendInput
                       |  connect: UserWhereUniqueInput
                       |  update: UserUpdateWithoutFriendDataInput
                       |  upsert: UserUpsertWithoutFriendInput
                       |}
                       |
                       |input UserUpdateOneRequiredWithoutFriendOfInput {
                       |  create: UserCreateWithoutFriendOfInput
                       |  connect: UserWhereUniqueInput
                       |  update: UserUpdateWithoutFriendOfDataInput
                       |  upsert: UserUpsertWithoutFriendOfInput
                       |}
                       |
                       |input UserUpdateWithoutFriendDataInput {
                       |  name: String
                       |  friendOf: UserUpdateOneRequiredWithoutFriendInput
                       |}
                       |
                       |input UserUpdateWithoutFriendOfDataInput {
                       |  name: String
                       |  friend: UserUpdateOneRequiredWithoutFriendOfInput
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

  "Sample schema with selfrelation and optional backrelation" should "be generated correctly" in {

    val project = SchemaDsl.fromString() {
      """type User{
        |   name: String! @unique
        |   bestBuddy: User
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input UserCreateInput {
                       |  name: String!
                       |  id: ID
                       |  bestBuddy: UserCreateOneInput
                       |}
                       |
                       |input UserCreateOneInput {
                       |  create: UserCreateInput
                       |  connect: UserWhereUniqueInput
                       |}
                       |
                       |input UserUpdateDataInput {
                       |  name: String
                       |  bestBuddy: UserUpdateOneInput
                       |}
                       |
                       |input UserUpdateInput {
                       |  name: String
                       |  bestBuddy: UserUpdateOneInput
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
                       |  name: String
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Disconnect and Delete" should "not be generated on required to-One Relations" in {

    val project = SchemaDsl.fromString() {
      """type Parent{
        |   name: String! @unique
        |   child: [Child]
        |}
        |
        |type Child{
        |   name: String! @unique
        |   parent: Parent!
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input ChildCreateInput {
                       |  name: String!
                       |  id: ID
                       |  parent: ParentCreateOneWithoutChildInput!
                       |}
                       |
                       |input ChildCreateManyWithoutParentInput {
                       |  create: [ChildCreateWithoutParentInput!]
                       |  connect: [ChildWhereUniqueInput!]
                       |}
                       |
                       |input ChildCreateWithoutParentInput {
                       |  name: String!
                       |  id: ID
                       |}
                       |
                       |input ChildUpdateInput {
                       |  name: String
                       |  parent: ParentUpdateOneRequiredWithoutChildInput
                       |}
                       |
                       |input ChildUpdateManyWithoutParentInput {
                       |  create: [ChildCreateWithoutParentInput!]
                       |  connect: [ChildWhereUniqueInput!]
                       |  set: [ChildWhereUniqueInput!]
                       |  disconnect: [ChildWhereUniqueInput!]
                       |  delete: [ChildWhereUniqueInput!]
                       |  update: [ChildUpdateWithWhereUniqueWithoutParentInput!]
                       |  updateMany: [ChildUpdateManyWithWhereNestedInput!]
                       |  deleteMany: [ChildScalarWhereInput!]
                       |  upsert: [ChildUpsertWithWhereUniqueWithoutParentInput!]
                       |}
                       |
                       |input ChildUpdateWithWhereUniqueWithoutParentInput {
                       |  where: ChildWhereUniqueInput!
                       |  data: ChildUpdateWithoutParentDataInput!
                       |}
                       |
                       |input ChildUpdateWithoutParentDataInput {
                       |  name: String
                       |}
                       |
                       |input ChildUpsertWithWhereUniqueWithoutParentInput {
                       |  where: ChildWhereUniqueInput!
                       |  update: ChildUpdateWithoutParentDataInput!
                       |  create: ChildCreateWithoutParentInput!
                       |}
                       |
                       |input ChildWhereUniqueInput {
                       |  name: String
                       |}
                       |
                       |input ParentCreateInput {
                       |  name: String!
                       |  id: ID
                       |  child: ChildCreateManyWithoutParentInput
                       |}
                       |
                       |input ParentCreateOneWithoutChildInput {
                       |  create: ParentCreateWithoutChildInput
                       |  connect: ParentWhereUniqueInput
                       |}
                       |
                       |input ParentCreateWithoutChildInput {
                       |  name: String!
                       |  id: ID
                       |}
                       |
                       |input ParentUpdateInput {
                       |  name: String
                       |  child: ChildUpdateManyWithoutParentInput
                       |}
                       |
                       |input ParentUpdateOneRequiredWithoutChildInput {
                       |  create: ParentCreateWithoutChildInput
                       |  connect: ParentWhereUniqueInput
                       |  update: ParentUpdateWithoutChildDataInput
                       |  upsert: ParentUpsertWithoutChildInput
                       |}
                       |
                       |input ParentUpdateWithoutChildDataInput {
                       |  name: String
                       |}
                       |
                       |input ParentUpsertWithoutChildInput {
                       |  update: ParentUpdateWithoutChildDataInput!
                       |  create: ParentCreateWithoutChildInput!
                       |}
                       |
                       |input ParentWhereUniqueInput {
                       |  name: String
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "When a type is both required and non-required two separate types" should "be generated" in {

    val project = SchemaDsl.fromString() {
      """type A {
        |    field: Int @unique
        |}
        |
        |type B {
        |    field: Int @unique
        |    a: A!
        |}
        |
        |type C {
        |    field: Int @unique
        |    a: A
        |}"""
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input ACreateInput {
                       |  field: Int
                       |  id: ID
                       |}
                       |
                       |input ACreateOneInput {
                       |  create: ACreateInput
                       |  connect: AWhereUniqueInput
                       |}
                       |
                       |input AUpdateDataInput {
                       |  field: Int
                       |}
                       |
                       |input AUpdateInput {
                       |  field: Int
                       |}
                       |
                       |input AUpdateManyMutationInput {
                       |  field: Int
                       |}
                       |
                       |input AUpdateOneInput {
                       |  create: ACreateInput
                       |  connect: AWhereUniqueInput
                       |  disconnect: Boolean
                       |  delete: Boolean
                       |  update: AUpdateDataInput
                       |  upsert: AUpsertNestedInput
                       |}
                       |
                       |input AUpdateOneRequiredInput {
                       |  create: ACreateInput
                       |  connect: AWhereUniqueInput
                       |  update: AUpdateDataInput
                       |  upsert: AUpsertNestedInput
                       |}
                       |
                       |input AUpsertNestedInput {
                       |  update: AUpdateDataInput!
                       |  create: ACreateInput!
                       |}
                       |
                       |input BCreateInput {
                       |  field: Int
                       |  id: ID
                       |  a: ACreateOneInput!
                       |}
                       |
                       |input BUpdateInput {
                       |  field: Int
                       |  a: AUpdateOneRequiredInput
                       |}
                       |
                       |input BUpdateManyMutationInput {
                       |  field: Int
                       |}
                       |
                       |input CCreateInput {
                       |  field: Int
                       |  id: ID
                       |  a: ACreateOneInput
                       |}
                       |
                       |input CUpdateInput {
                       |  field: Int
                       |  a: AUpdateOneInput
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "When a type is both required and non-required two separate types" should "be generated (changed order)" in {

    val project = SchemaDsl.fromString() {
      """type A {
        |    field: Int @unique
        |}
        |
        |type B {
        |    field: Int @unique
        |    a: A
        |}
        |
        |type C {
        |    field: Int @unique
        |    a: A!
        |}"""
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes =
      """input ACreateInput {
        |  field: Int
        |  id: ID
        |}
        |
        |input ACreateOneInput {
        |  create: ACreateInput
        |  connect: AWhereUniqueInput
        |}
        |
        |input AUpdateDataInput {
        |  field: Int
        |}
        |
        |input AUpdateInput {
        |  field: Int
        |}
        |
        |input AUpdateOneInput {
        |  create: ACreateInput
        |  connect: AWhereUniqueInput
        |  disconnect: Boolean
        |  delete: Boolean
        |  update: AUpdateDataInput
        |  upsert: AUpsertNestedInput
        |}
        |
        |input AUpdateOneRequiredInput {
        |  create: ACreateInput
        |  connect: AWhereUniqueInput
        |  update: AUpdateDataInput
        |  upsert: AUpsertNestedInput
        |}
        |
        |input AUpsertNestedInput {
        |  update: AUpdateDataInput!
        |  create: ACreateInput!
        |}
        |
        |input BCreateInput {
        |  field: Int
        |  id: ID
        |  a: ACreateOneInput
        |}
        |
        |input BUpdateInput {
        |  field: Int
        |  a: AUpdateOneInput
        |}
        |
        |input CCreateInput {
        |  field: Int
        |  id: ID
        |  a: ACreateOneInput!
        |}
        |
        |input CUpdateInput {
        |  field: Int
        |  a: AUpdateOneRequiredInput
        |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Nested Create types" should "not be omitted anymore since we now have bring your own id" in {
    val project = SchemaDsl.fromString() {
      """type A {
        |    id: ID! @unique
        |    b: B
        |}
        |
        |type B {
        |    a: A!
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString
    schema should containInputType("BCreateOneWithoutAInput")
  }

  "Sample schema with relation and relation strategy NONE" should "be generated correctly" in {

    val project = SchemaDsl.fromStringV11() {

      """type User {
        |  id: ID! @id(strategy:NONE)
        |  name: String!
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input UserCreateInput {
                       |  id: ID!
                       |  name: String!
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Sample schema with relation and relation strategy AUTO" should "be generated correctly" in {

    val project = SchemaDsl.fromStringV11() {

      """type User {
        |  id: ID! @id(strategy:AUTO)
        |  name: String!
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input UserCreateInput {
                       |  id: ID
                       |  name: String!
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }
  //Fixme once AUTO and idtypes Int, CUID, UUID are explicitly allowed, add them
}
