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
}
