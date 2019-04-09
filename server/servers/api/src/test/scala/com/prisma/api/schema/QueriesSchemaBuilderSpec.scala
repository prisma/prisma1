package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.ConnectorCapability.NodeQueryCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class QueriesSchemaBuilderSpec extends WordSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the single item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl.fromStringV11() {
        """
          |type Todo {
          |  id: ID! @id
          |}
        """.stripMargin
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containQuery("todo(where: TodoWhereUniqueInput!): Todo")
    }

    "be present if there is a unique field other than ID" in {
      val project = SchemaDsl.fromStringV11() {
        """
          |type Todo {
          |  id: ID! @id
          |  test: String @unique
          |}
        """.stripMargin
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containQuery("todo")
    }
  }

  "the multi item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl.fromStringV11() {
        """
          |type Todo {
          |  id: ID! @id
          |}
        """.stripMargin
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containQuery(
        "todoes(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [Todo]!"
      )
    }
  }

  "the many item connection query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl.fromStringV11() {
        """
          |type Todo {
          |  id: ID! @id
          |}
        """.stripMargin
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema should containQuery(
        "todoesConnection(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): TodoConnection!"
      )

      schema should containType("TodoConnection", fields = Vector("pageInfo: PageInfo!", "edges: [TodoEdge]!", "aggregate: AggregateTodo!"))
      schema should containType("TodoEdge", fields = Vector("node: Todo!", "cursor: String!"))
    }
  }

  "the node query for a model" must {
    "be present if the connector has the capability" in {
      val project = SchemaDsl.fromStringV11() {
        """
          |type Todo {
          |  id: ID! @id
          |}
        """.stripMargin
      }

      val schemaBuilder = SchemaBuilderImpl(project, capabilities = ConnectorCapabilities(NodeQueryCapability))(testDependencies)
      val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())
      schema should include("node(")
      schema should include("id: ID!): Node")
    }

    "not be present if the connector doesn't have the capability" in {
      val project = SchemaDsl.fromStringV11() {
        """
          |type Todo {
          |  id: ID! @id
          |}
        """.stripMargin
      }

      val schemaBuilder = SchemaBuilderImpl(project)(testDependencies)
      val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

      schema should not(include("node("))
      schema should not(include("id: ID!): Node"))
    }
  }
}
