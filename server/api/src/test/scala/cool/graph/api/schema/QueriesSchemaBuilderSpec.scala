package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

import scala.collection.mutable

class QueriesSchemaBuilderSpec extends WordSpec with Matchers with ApiBaseSpec with GraphQLSchemaAssertions {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the single item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      val query = schema.mustContainQuery("todo")
      query should be("todo(where: TodoWhereUniqueInput!): Todo")
    }
  }

  "the multi item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      val query = schema.mustContainQuery("todoes")
      println(query)
      query should be("todoes(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [Todo]!")
    }

    "not include a *WhereUniqueInput if there is no unique field" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo").copy(fields = mutable.Buffer.empty).field("test", _.String)
      }

      println(project)

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      //      val query = schema.mustContainQuery("todoes")
      println(schema)
      //      query should be("todoes(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [Todo]!")
    }
  }
}
