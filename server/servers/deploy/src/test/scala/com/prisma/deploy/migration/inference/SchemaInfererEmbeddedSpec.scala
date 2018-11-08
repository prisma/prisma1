package com.prisma.deploy.migration.inference

import com.prisma.deploy.connector.InferredTables
import com.prisma.deploy.migration.validation.SchemaSyntaxValidator
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.{OnDelete, Schema}
import com.prisma.shared.schema_dsl.TestProject
import org.scalatest.{Matchers, WordSpec}

class SchemaInfererEmbeddedSpec extends WordSpec with Matchers with DeploySpecBase {

  val inferer      = SchemaInferrer(Set.empty)
  val emptyProject = TestProject.empty

  "Inferring embedded typeDirectives" should {
    "work if one side provides embedded" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!]! @relation(name:"MyRelationName")
          |}
          |
          |type Comment @embedded {
          |  todo: Todo!
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelBName should equal("Todo")
      schema.models should have(size(2))
      val todo = schema.getModelByName_!("Todo")
      todo.isEmbedded should be(false)
      val comment = schema.getModelByName_!("Comment")
      comment.isEmbedded should be(true)
    }
  }

  //Fixme more ideas for test cases / spec work
//  schema with only embedded types
//  embedded type in other embedded type
//  non-embedded type within embedded type
//  self relations between embedded types
//  embedded types on connector without embedded types capability

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty): Schema = {
    val validator = SchemaSyntaxValidator(
      types,
      SchemaSyntaxValidator.directiveRequirements,
      deployConnector.fieldRequirements,
      Set.empty
    )

    val prismaSdl = validator.generateSDL

    inferer.infer(schema, SchemaMapping.empty, prismaSdl, InferredTables.empty)
  }
}
