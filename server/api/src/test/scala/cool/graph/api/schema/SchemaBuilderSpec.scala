package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class SchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaAssertions {

  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the create Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("tag", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("createTodo")
    mutation should be("createTodo(data: TodoCreateInput!): Todo!")

    val inputType = schema.mustContainInputType("TodoCreateInput")
    inputType should be("""input TodoCreateInput {
                          |  title: String!
                          |  tag: String
                          |}""".stripMargin)
  }
}
