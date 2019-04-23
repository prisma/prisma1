package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, IdSequenceCapability, IntIdCapability, SupportsExistingDatabasesCapability}
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class InputTypesSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder
//  override def doNotRunForCapabilities = Set(EmbeddedTypesCapability)

  "Sample schema with relation and id only types" should "be generated correctly" in {

    val project = SchemaDsl.fromStringV11() {

      """type User {
        |  id: ID! @id
        |  name: String!
        |}
        |
        |type B {
        |  id: ID! @id
        |  rel: User @relation(link: INLINE)
        |  c: C @relation(link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
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

    val project = SchemaDsl.fromStringV11() {

      """type User {
        |  id: ID! @id
        |  name: String!
        |}
        |
        |type B {
        |  id: ID! @id
        |  rel: User @relation(link: INLINE)
        |  c: C @relation(link: INLINE)
        |}
        |
        |type C {
        |  id: ID! @id
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
        |  connect: BWhereUniqueInput
        |}
        |
        |input BCreateWithoutCInput {
        |  id: ID
        |  rel: UserCreateOneInput
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
        |input CCreateInput {
        |  id: ID
        |  name: String!
        |  b: BCreateOneWithoutCInput
        |}
        |
        |input CCreateOneWithoutBInput {
        |  create: CCreateWithoutBInput
        |  connect: CWhereUniqueInput
        |}
        |
        |input CCreateWithoutBInput {
        |  id: ID
        |  name: String!
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

    val project = SchemaDsl.fromStringV11() {

      """type User{
        |   id: ID! @id
        |   name: String! @unique
        |   friend: User! @relation(name: "UserFriends" link: INLINE)
        |   friendOf: User! @relation(name: "UserFriends")
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input UserCreateInput {
                       |  id: ID
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
                       |  id: ID
                       |  name: String!
                       |  friendOf: UserCreateOneWithoutFriendInput!
                       |}
                       |
                       |input UserCreateWithoutFriendOfInput {
                       |  id: ID
                       |  name: String!
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
                       |  id: ID
                       |  name: String
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Sample schema with selfrelation and optional backrelation" should "be generated correctly" in {

    val project = SchemaDsl.fromStringV11() {
      """type User{
        |   id: ID! @id
        |   name: String! @unique
        |   bestBuddy: User @relation(link: INLINE)
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input UserCreateInput {
                       |  id: ID
                       |  name: String!
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
                       |  id: ID
                       |  name: String
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Disconnect and Delete" should "not be generated on required to-One Relations" in {

    val project = SchemaDsl.fromStringV11() {
      """type Parent{
        |   id: ID! @id
        |   name: String! @unique
        |   child: [Child]
        |}
        |
        |type Child{
        |   id: ID! @id
        |   name: String! @unique
        |   parent: Parent! @relation(link: INLINE)
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input ChildCreateInput {
                       |  id: ID
                       |  name: String!
                       |  parent: ParentCreateOneWithoutChildInput!
                       |}
                       |
                       |input ChildCreateManyWithoutParentInput {
                       |  create: [ChildCreateWithoutParentInput!]
                       |  connect: [ChildWhereUniqueInput!]
                       |}
                       |
                       |input ChildCreateWithoutParentInput {
                       |  id: ID
                       |  name: String!
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
                       |  id: ID
                       |  name: String
                       |}
                       |
                       |input ParentCreateInput {
                       |  id: ID
                       |  name: String!
                       |  child: ChildCreateManyWithoutParentInput
                       |}
                       |
                       |input ParentCreateOneWithoutChildInput {
                       |  create: ParentCreateWithoutChildInput
                       |  connect: ParentWhereUniqueInput
                       |}
                       |
                       |input ParentCreateWithoutChildInput {
                       |  id: ID
                       |  name: String!
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
                       |  id: ID
                       |  name: String
                       |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "When a type is both required and non-required two separate types" should "be generated" in {

    val project = SchemaDsl.fromStringV11() {
      """type A {
        |    id: ID! @id
        |    field: Int @unique
        |}
        |
        |type B {
        |    id: ID! @id
        |    field: Int @unique
        |    a: A! @relation(link: INLINE)
        |}
        |
        |type C {
        |    id: ID! @id
        |    field: Int @unique
        |    a: A @relation(link: INLINE)
        |}"""
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes = """input ACreateInput {
                       |  id: ID
                       |  field: Int
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
                       |  id: ID
                       |  field: Int
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
                       |  id: ID
                       |  field: Int
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

    val project = SchemaDsl.fromStringV11() {
      """type A {
        |    id: ID! @id
        |    field: Int @unique
        |}
        |
        |type B {
        |    id: ID! @id
        |    field: Int @unique
        |    a: A @relation(link: INLINE)
        |}
        |
        |type C {
        |    id: ID! @id
        |    field: Int @unique
        |    a: A! @relation(link: INLINE)
        |}"""
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    val inputTypes =
      """input ACreateInput {
        |  id: ID
        |  field: Int
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
        |  id: ID
        |  field: Int
        |  a: ACreateOneInput
        |}
        |
        |input BUpdateInput {
        |  field: Int
        |  a: AUpdateOneInput
        |}
        |
        |input CCreateInput {
        |  id: ID
        |  field: Int
        |  a: ACreateOneInput!
        |}
        |
        |input CUpdateInput {
        |  field: Int
        |  a: AUpdateOneRequiredInput
        |}"""

    inputTypes.split("input").map(inputType => schema should include(inputType.stripMargin))
  }

  "Nested Create types" should "with only id and relation should still be there because of bring your own id" in {
    val project = SchemaDsl.fromStringV11() {
      """type A {
        |    id: ID! @id
        |    b: B @relation(link: INLINE)
        |}
        |
        |type B {
        |    id: ID! @id
        |    a: A!
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString
    schema should containInputType(
      name = "BCreateWithoutAInput",
      fields = Vector("id: ID")
    )
  }

  "Sample schema with relation and id strategy NONE" should "be generated correctly" in {

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

  "Sample schema with relation and id strategy AUTO" should "be generated correctly" in {

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

  "Sample schema with relation and id strategy SEQUENCE" should "be generated correctly" in {

    val project = SchemaDsl.fromStringV11Capabilities(Set(IdSequenceCapability, IntIdCapability)) {

      """type User {
        |  id: Int! @id(strategy: SEQUENCE)
        |}""".stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project)).toString

    schema should not(containInputType("UserCreateInput"))
  }
  //Fixme once AUTO and idtypes CUID, UUID are explicitly allowed, add them
}
