package com.prisma.deploy.migration.inference

import com.prisma.deploy.connector.InferredTables
import com.prisma.deploy.migration.validation.DataModelValidatorImpl
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.ApiConnectorCapability.{EmbeddedTypesCapability, MongoRelationsCapability}
import com.prisma.shared.models.{ConnectorCapability, Schema}
import com.prisma.shared.schema_dsl.TestProject
import org.scalatest.{Matchers, WordSpec}

class SchemaInfererEmbeddedSpec extends WordSpec with Matchers with DeploySpecBase {
  val emptyProject = TestProject.empty

  "Inferring embedded typeDirectives" should {
    "work if one side provides embedded" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment!]! @relation(name: "MyRelationName", strategy: EMBED)
          |}
          |
          |type Comment @embedded {
          |  test: String
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types, capabilities = Set(EmbeddedTypesCapability, MongoRelationsCapability))

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

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty, capabilities: Set[ConnectorCapability]): Schema = {
    val prismaSdl = DataModelValidatorImpl.validate(types, deployConnector.fieldRequirements, capabilities).get
    SchemaInferrer(capabilities).infer(schema, mapping, prismaSdl, InferredTables.empty)
  }
}
