package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class QueriesSchemaBuilderSpec extends WordSpec with Matchers with ApiBaseSpec with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the single item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containQuery("todo(where: TodoWhereUniqueInput!): Todo")
    }

    "not be present if there is no unique field" in {
      val project = SchemaDsl() { schema =>
        val testSchema = schema.model("Todo")
        testSchema.fields.clear()
        testSchema.field("id", _.GraphQLID, isUnique = true, isHidden = true)
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema shouldNot containQuery("todo")
    }

    "be present if there is a unique field other than ID" in {
      val project = SchemaDsl() { schema =>
        val testSchema = schema.model("Todo")
        testSchema.fields.clear()
        testSchema.field("id", _.GraphQLID, isUnique = true, isHidden = true)
        testSchema.field("test", _.String, isUnique = true)
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containQuery("todo")
    }
  }

  "the multi item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containQuery(
        "todoes(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [Todo]!"
      )
    }
  }

  "the many item connection query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema should containQuery(
        "todoesConnection(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): TodoConnection!"
      )

      schema should containType("TodoConnection", fields = Vector("pageInfo: PageInfo!", "edges: [TodoEdge!]"))
      schema should containType("TodoEdge", fields = Vector("node: Todo!", "cursor: String!"))
    }
  }
}
