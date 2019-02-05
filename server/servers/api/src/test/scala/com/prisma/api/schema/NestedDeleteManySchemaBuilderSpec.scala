package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class NestedDeleteManySchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)
  val schemaBuilder                   = testDependencies.apiSchemaBuilder

  "Nested DeleteMany" should "have scalarWhereFilter" in {
    val project = SchemaDsl.fromString() {
      """
        |type Top {
        |   name: String @unique
        |   other: [Other]
        |}
        |
        |type Other{
        |   top: Top
        |   test: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = ConnectorCapabilities(EmbeddedTypesCapability))(testDependencies)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should include("input TopUpdateInput {\n  name: String\n  other: OtherUpdateManyWithoutTopInput\n}")
    schema should include(
      "input OtherUpdateManyWithoutTopInput {\n  create: [OtherCreateWithoutTopInput!]\n  updateMany: [OtherUpdateManyWithWhereNestedInput!]\n  deleteMany: [OtherScalarWhereInput!]\n}")
  }

  "Nested DeleteMany" should "not be created if there are no scalar fields" in {
    val project = SchemaDsl.fromString() {
      """
        |type Top {
        |   name: String @unique
        |   other: [Other]
        |}
        |
        |type Other{
        |   top: Top
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = ConnectorCapabilities(EmbeddedTypesCapability))(testDependencies)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should include("input TopUpdateInput {\n  name: String\n}")
    schema should not(include("OtherScalarWhereInput"))
  }

}
