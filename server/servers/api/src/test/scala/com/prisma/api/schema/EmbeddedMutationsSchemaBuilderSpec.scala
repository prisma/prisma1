package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class EmbeddedMutationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)
  val schemaBuilder                   = testDependencies.apiSchemaBuilder

  "An embedded type" should "not produce mutations in the schema" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Embedded @embedded {
        |   name: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = ConnectorCapabilities(EmbeddedTypesCapability))(testDependencies)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should not(include("type Mutation {"))
  }

  "An embedded type" should "not have disconnect and connect mutations when nested" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Top {
        |   id: ID! @id
        |   name: String @unique
        |   embedded: Embedded
        |}
        |
        |type Embedded @embedded {
        |   name: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = ConnectorCapabilities(EmbeddedTypesCapability))(testDependencies)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should include(
      "input EmbeddedUpdateOneInput {\n  create: EmbeddedCreateInput\n  delete: Boolean\n  update: EmbeddedUpdateDataInput\n  upsert: EmbeddedUpsertNestedInput\n}")
  }
}
