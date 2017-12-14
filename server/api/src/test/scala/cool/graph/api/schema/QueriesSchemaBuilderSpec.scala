package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class QueriesSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaAssertions {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the single item query for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo")
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val query = schema.mustContainQuery("todo")
    query should be("todo(where: TodoWhereUniqueInput!): Todo")
  }
}
