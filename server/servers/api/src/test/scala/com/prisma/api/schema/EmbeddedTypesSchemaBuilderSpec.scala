package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ApiConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class EmbeddedTypesSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)
  val schemaBuilder                   = testDependencies.apiSchemaBuilder

  "An embedded relation" should "have relational filters, a join relation should not" in {
    val project = SchemaDsl.fromString() {

      """
        |type Top2{
        |   id: ID! @unique
        |   top: Top
        |}
        |
        |type Top{
        |   id: ID! @unique
        |   em: Embedded
        |}
        |
        |type Embedded @embedded {
        |   name: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = Set(EmbeddedTypesCapability))(testDependencies, system)
    val build         = schemaBuilder.build()
    val schema        = SchemaRenderer.renderSchema(build)

    schema should include("""input TopWhereInput {
                            |  "Logical AND on all given filters."
                            |  AND: [TopWhereInput!]
                            |
                            |  "Logical OR on all given filters."
                            |  OR: [TopWhereInput!]
                            |
                            |  "Logical NOT on all given filters combined by AND."
                            |  NOT: [TopWhereInput!]
                            |  id: ID
                            |
                            |  "All values that are not equal to given value."
                            |  id_not: ID
                            |
                            |  "All values that are contained in given list."
                            |  id_in: [ID!]
                            |
                            |  "All values that are not contained in given list."
                            |  id_not_in: [ID!]
                            |
                            |  "All values less than the given value."
                            |  id_lt: ID
                            |
                            |  "All values less than or equal the given value."
                            |  id_lte: ID
                            |
                            |  "All values greater than the given value."
                            |  id_gt: ID
                            |
                            |  "All values greater than or equal the given value."
                            |  id_gte: ID
                            |
                            |  "All values containing the given string."
                            |  id_contains: ID
                            |
                            |  "All values not containing the given string."
                            |  id_not_contains: ID
                            |
                            |  "All values starting with the given string."
                            |  id_starts_with: ID
                            |
                            |  "All values not starting with the given string."
                            |  id_not_starts_with: ID
                            |
                            |  "All values ending with the given string."
                            |  id_ends_with: ID
                            |
                            |  "All values not ending with the given string."
                            |  id_not_ends_with: ID
                            |  em: EmbeddedWhereInput
                            |}""".stripMargin)

    schema should include("""input Top2WhereInput {
                            |  "Logical AND on all given filters."
                            |  AND: [Top2WhereInput!]
                            |
                            |  "Logical OR on all given filters."
                            |  OR: [Top2WhereInput!]
                            |
                            |  "Logical NOT on all given filters combined by AND."
                            |  NOT: [Top2WhereInput!]
                            |  id: ID
                            |
                            |  "All values that are not equal to given value."
                            |  id_not: ID
                            |
                            |  "All values that are contained in given list."
                            |  id_in: [ID!]
                            |
                            |  "All values that are not contained in given list."
                            |  id_not_in: [ID!]
                            |
                            |  "All values less than the given value."
                            |  id_lt: ID
                            |
                            |  "All values less than or equal the given value."
                            |  id_lte: ID
                            |
                            |  "All values greater than the given value."
                            |  id_gt: ID
                            |
                            |  "All values greater than or equal the given value."
                            |  id_gte: ID
                            |
                            |  "All values containing the given string."
                            |  id_contains: ID
                            |
                            |  "All values not containing the given string."
                            |  id_not_contains: ID
                            |
                            |  "All values starting with the given string."
                            |  id_starts_with: ID
                            |
                            |  "All values not starting with the given string."
                            |  id_not_starts_with: ID
                            |
                            |  "All values ending with the given string."
                            |  id_ends_with: ID
                            |
                            |  "All values not ending with the given string."
                            |  id_not_ends_with: ID
                            |}""".stripMargin)
  }

  "An embedded relation" should "have relational filters, a join relation should not 2 " in {
    val project = SchemaDsl.fromString() {

      """type Top{
        |   id: ID! @unique
        |   em: [Embedded!]!
        |}
        |
        |type Embedded @embedded {
        |   name: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = Set(EmbeddedTypesCapability))(testDependencies, system)
    val build         = schemaBuilder.build()
    val schema        = SchemaRenderer.renderSchema(build)

    schema should include("""type Top implements Node {
                            |  id: ID!
                            |  em: [Embedded!]
                            |}""".stripMargin)
  }

  "An embedded type" should "not have relational filters towards another top level type " in {
    val project = SchemaDsl.fromString() {

      """type User {
        |  id: ID! @unique
        |  name: String!
        |  em: Embe!
        |}
        |
        |  type Embe @embedded {
        |  s: String!
        |  a: A
        |}
        |
        |  type A {
        |  id: ID! @unique
        |  s2: String!
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = Set(EmbeddedTypesCapability))(testDependencies, system)
    val build         = schemaBuilder.build()
    val schema        = SchemaRenderer.renderSchema(build)

    schema should include("""input UserWhereInput {
                            |  "Logical AND on all given filters."
                            |  AND: [UserWhereInput!]
                            |
                            |  "Logical OR on all given filters."
                            |  OR: [UserWhereInput!]
                            |
                            |  "Logical NOT on all given filters combined by AND."
                            |  NOT: [UserWhereInput!]
                            |  id: ID
                            |
                            |  "All values that are not equal to given value."
                            |  id_not: ID
                            |
                            |  "All values that are contained in given list."
                            |  id_in: [ID!]
                            |
                            |  "All values that are not contained in given list."
                            |  id_not_in: [ID!]
                            |
                            |  "All values less than the given value."
                            |  id_lt: ID
                            |
                            |  "All values less than or equal the given value."
                            |  id_lte: ID
                            |
                            |  "All values greater than the given value."
                            |  id_gt: ID
                            |
                            |  "All values greater than or equal the given value."
                            |  id_gte: ID
                            |
                            |  "All values containing the given string."
                            |  id_contains: ID
                            |
                            |  "All values not containing the given string."
                            |  id_not_contains: ID
                            |
                            |  "All values starting with the given string."
                            |  id_starts_with: ID
                            |
                            |  "All values not starting with the given string."
                            |  id_not_starts_with: ID
                            |
                            |  "All values ending with the given string."
                            |  id_ends_with: ID
                            |
                            |  "All values not ending with the given string."
                            |  id_not_ends_with: ID
                            |  name: String
                            |
                            |  "All values that are not equal to given value."
                            |  name_not: String
                            |
                            |  "All values that are contained in given list."
                            |  name_in: [String!]
                            |
                            |  "All values that are not contained in given list."
                            |  name_not_in: [String!]
                            |
                            |  "All values less than the given value."
                            |  name_lt: String
                            |
                            |  "All values less than or equal the given value."
                            |  name_lte: String
                            |
                            |  "All values greater than the given value."
                            |  name_gt: String
                            |
                            |  "All values greater than or equal the given value."
                            |  name_gte: String
                            |
                            |  "All values containing the given string."
                            |  name_contains: String
                            |
                            |  "All values not containing the given string."
                            |  name_not_contains: String
                            |
                            |  "All values starting with the given string."
                            |  name_starts_with: String
                            |
                            |  "All values not starting with the given string."
                            |  name_not_starts_with: String
                            |
                            |  "All values ending with the given string."
                            |  name_ends_with: String
                            |
                            |  "All values not ending with the given string."
                            |  name_not_ends_with: String
                            |  em: EmbeWhereInput
                            |}""".stripMargin)

    schema should include("""input EmbeWhereInput {
                            |  "Logical AND on all given filters."
                            |  AND: [EmbeWhereInput!]
                            |
                            |  "Logical OR on all given filters."
                            |  OR: [EmbeWhereInput!]
                            |
                            |  "Logical NOT on all given filters combined by AND."
                            |  NOT: [EmbeWhereInput!]
                            |  s: String
                            |
                            |  "All values that are not equal to given value."
                            |  s_not: String
                            |
                            |  "All values that are contained in given list."
                            |  s_in: [String!]
                            |
                            |  "All values that are not contained in given list."
                            |  s_not_in: [String!]
                            |
                            |  "All values less than the given value."
                            |  s_lt: String
                            |
                            |  "All values less than or equal the given value."
                            |  s_lte: String
                            |
                            |  "All values greater than the given value."
                            |  s_gt: String
                            |
                            |  "All values greater than or equal the given value."
                            |  s_gte: String
                            |
                            |  "All values containing the given string."
                            |  s_contains: String
                            |
                            |  "All values not containing the given string."
                            |  s_not_contains: String
                            |
                            |  "All values starting with the given string."
                            |  s_starts_with: String
                            |
                            |  "All values not starting with the given string."
                            |  s_not_starts_with: String
                            |
                            |  "All values ending with the given string."
                            |  s_ends_with: String
                            |
                            |  "All values not ending with the given string."
                            |  s_not_ends_with: String
                            |}
                            |""".stripMargin)

  }

}
