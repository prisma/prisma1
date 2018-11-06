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

  "An embedded type" should "should have the @embedded in the schema" in {
    val project = SchemaDsl.fromString() {
      """
        |type Embedded @embedded {
        |   name: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = Set(EmbeddedTypesCapability))(testDependencies, system)
    val build         = schemaBuilder.build()
    val schema        = SchemaRenderer.renderSchema(build)

    schema should include("type Embedded @embedded {\n  name: String\n}")
  }

//relational filters only on embedded types

}
