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

    // do not include a node interface without id

    "not include a *WhereUniqueInput if there is no visible unique field" in {
      val project = SchemaDsl() { schema =>
        val testSchema = schema.model("Todo")
        testSchema.fields.clear()
        testSchema.field("id", _.GraphQLID, isUnique = true, isHidden = true)
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema shouldNot containType("Todo", "Node")
      schema shouldNot containType("TodoWhereUniqueInput")
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

      schema should containType("TodoConnection")
      schema should containField("TodoConnection", "pageInfo: PageInfo!")
      schema should containField("TodoConnection", "edges: [TodoEdge!]")

      schema should containType("TodoEdge")
      schema should containField("TodoEdge", "node: Todo!")
      schema should containField("TodoEdge", "cursor: String!")
    }

    // no create input if no other field
  }
}
