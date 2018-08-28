package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class EmbeddedMutationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {

  override def onlyRunSuiteForMongo: Boolean = true
  val schemaBuilder                          = testDependencies.apiSchemaBuilder

  "An embedded type" should "not produce mutations in the schema" in {
    val project = SchemaDsl.fromString() {
      """
        |type Embedded @embedded {
        |   name: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = Vector(EmbeddedTypesCapability))(testDependencies, system)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should not(include("type Mutation {"))
  }
}
