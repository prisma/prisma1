package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class GeneralSchemaBuilderSpec extends WordSpec with Matchers with ApiBaseSpec with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  def projectWithHiddenID: Project = {
    SchemaDsl() { schema =>
      val testSchema = schema.model("Todo")
      testSchema.fields.clear()
      testSchema.field("id", _.GraphQLID, isUnique = true, isHidden = true).field("someOtherField", _.Int)
    }
  }

  "The types of a schema" must {
    "be generated without a Node interface if there is no visible ID field" in {
      val project = projectWithHiddenID
      val schema  = SchemaRenderer.renderSchema(schemaBuilder(project))
      println(schema)
      schema should containType("Todo")
      schema shouldNot containType("Todo", "Node")
    }

    "not include a *WhereUniqueInput if there is no visible unique field" in {
      val project = projectWithHiddenID
      val schema  = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema shouldNot containType("TodoWhereUniqueInput")
    }

    "not include a *CreateInput if there are no fields / only hidden fields" in {
      val project = projectWithHiddenID
      val schema  = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema shouldNot containType("TodoCreateInput")
    }

    "not include a *CreateInput if there is only an ID field" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema shouldNot containType("TodoCreateInput")
    }
  }
}
