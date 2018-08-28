package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class EmbeddedQueriesSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {

  override def onlyRunSuiteForMongo: Boolean = true
  val schemaBuilder                          = testDependencies.apiSchemaBuilder

  "An embedded type" should "not produce queries in the schema" in {
    val project = SchemaDsl.fromString() {
      """
          |type Embedded @embedded {
          |   name: String
          |}
        """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = Vector.empty)(testDependencies, system)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should not(include("type Query {"))
  }
}
