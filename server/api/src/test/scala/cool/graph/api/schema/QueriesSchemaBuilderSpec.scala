package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

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
      query should be("todoes(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [Todo]!")
    }

    "not include a *WhereUniqueInput if there is no visible unique field" in {
      val project = SchemaDsl() { schema =>
        val testSchema = schema.model("Todo")
        testSchema.fields.clear()
        testSchema.field("id", _.GraphQLID, isUnique = true, isHidden = true)
        testSchema.field("test", _.String)
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema shouldNot include("type Todo implements Node")
    }
  }

  "the many item connection query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      val query = schema.mustContainQuery("todoesConnection")
      query should be(
        "todoesConnection(where: TodoWhereInput, orderBy: TodoOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): TodoConnection!")

      val connectionType = schema.mustContainType("TodoConnection")

      mustBeEqual(
        connectionType,
        """type TodoConnection {
          |  # Information to aid in pagination.
          |  pageInfo: PageInfo!
          |
          |  # A list of edges.
          |  edges: [TodoEdge!]
          |}""".stripMargin
      )

      val edgeType = schema.mustContainType("TodoEdge")
//      mustBeEqual(
//        edgeType,
//        """type TodoEdge {
//          |
//          |}
//        """.stripMargin
//      )

      //val aggregateType = schema.mustContainType("TodoAggregate")
      //val groupByType   = schema.mustContainType("TodoGroupBy")
    }
  }
}
