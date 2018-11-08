package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class GeneralSchemaBuilderSpec extends WordSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  def projectWithHiddenID: Project = {
    SchemaDsl.fromBuilder { schema =>
      val testSchema = schema.model("Todo")
      testSchema.fields.clear()
      testSchema.field("id", _.Cuid, isUnique = true, isHidden = true).field("someOtherField", _.Int)
    }
  }

  "The types of a schema" must {
    "be generated without a Node interface if there is no visible ID field" in {
      val project = projectWithHiddenID
      val schema  = SchemaRenderer.renderSchema(schemaBuilder(project))
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
      val project = SchemaDsl.fromBuilder { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      schema shouldNot containType("TodoCreateInput")
    }

    "not include the *PreviousValues type if there a type has no scalar field" in {
      val project = SchemaDsl.fromBuilder { schema =>
        val todo    = schema.model("Todo")
        val comment = schema.model("Comment")
        todo.fields.clear()
        todo.field("id", _.Cuid, isUnique = true, isHidden = true).oneToManyRelation("comments", "todo", comment)
      }
      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema shouldNot include("TodoPreviousValues")
    }

    "must not crash when using type names that can not be pluralized" in {
      // this is a limitation of our pluralization lib: https://github.com/atteo/evo-inflector/blob/64ab921bbdeb797e6aa6469eb7a53320b505221a/src/main/java/org/atteo/evo/inflector/English.java#L133
      val project = SchemaDsl.fromBuilder { schema =>
        schema.model("News")
        schema.model("Homework")
        schema.model("Scissors")
      }
      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should include("news(where: ")
      schema should include("newses(where: ")
      schema should include("homework(where: ")
      schema should include("homeworks(where: ")
      schema should include("scissors(where: ")
      schema should include("scissorses(where: ")
    }
  }
}
