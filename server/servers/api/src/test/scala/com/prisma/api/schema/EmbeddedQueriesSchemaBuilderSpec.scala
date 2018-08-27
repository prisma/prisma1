package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.api.connector.NodeQueryCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class EmbeddedQueriesSchemaBuilderSpec extends WordSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  override def onlyRunSuiteForMongo: Boolean = true

  "An embedded type" must {
    "must not produce queries in the schema" in {
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
}
