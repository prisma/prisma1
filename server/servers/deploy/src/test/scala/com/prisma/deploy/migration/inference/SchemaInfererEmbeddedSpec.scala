package com.prisma.deploy.migration.inference

import com.prisma.deploy.connector.InferredTables
import com.prisma.deploy.migration.validation.DataModelValidatorImpl
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.ConnectorCapability.{EmbeddedTypesCapability, RelationLinkListCapability}
import com.prisma.shared.models.Manifestations.{EmbeddedRelationLink, RelationTable}
import com.prisma.shared.models.{ConnectorCapabilities, ConnectorCapability, Schema}
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
          |  comments: [Comment] @relation(name: "MyRelationName")
          |}
          |
          |type Comment @embedded {
          |  test: String
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types, capabilities = ConnectorCapabilities(EmbeddedTypesCapability, RelationLinkListCapability))

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelBName should equal("Todo")
      schema.models should have(size(2))
      val todo = schema.getModelByName_!("Todo")
      todo.isEmbedded should be(false)
      val comment = schema.getModelByName_!("Comment")
      comment.isEmbedded should be(true)
      relation.template.manifestation should be(None)
    }

    "work if one with a relation from embedded to non-parent non embedded type" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name: "MyRelationName")
          |}
          |
          |type Comment @embedded {
          |  test: String
          |  other: Other @relation(name: "MyOtherRelationName")
          |}
          |
          |type Other {
          |  id: ID! @id
          |}""".stripMargin.trim()
      val schema = infer(emptyProject.schema, types, capabilities = ConnectorCapabilities(EmbeddedTypesCapability, RelationLinkListCapability))

      schema.relations should have(size(2))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelBName should equal("Todo")
      relation.template.manifestation should be(None)

      val relation2 = schema.getRelationByName_!("MyOtherRelationName")
      relation2.modelAName should equal("Comment")
      relation2.modelBName should equal("Other")
      relation2.manifestation should be(EmbeddedRelationLink("Comment", "other"))

      schema.models should have(size(3))
      val todo = schema.getModelByName_!("Todo")
      todo.isEmbedded should be(false)
      val comment = schema.getModelByName_!("Comment")
      comment.isEmbedded should be(true)
      val other = schema.getModelByName_!("Other")
      other.isEmbedded should be(false)
    }

    "work if one with a relation from embedded to embedded type" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name: "MyRelationName")
          |}
          |
          |type Comment @embedded {
          |  test: String
          |  other: Other @relation(name: "MyOtherRelationName")
          |}
          |
          |type Other @embedded{
          |  test: String
          |}""".stripMargin.trim()
      val schema = infer(emptyProject.schema, types, capabilities = ConnectorCapabilities(EmbeddedTypesCapability, RelationLinkListCapability))

      schema.relations should have(size(2))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelBName should equal("Todo")
      relation.template.manifestation should be(None)

      val relation2 = schema.getRelationByName_!("MyOtherRelationName")
      relation2.modelAName should equal("Comment")
      relation2.modelBName should equal("Other")
      relation2.template.manifestation should be(None)

      schema.models should have(size(3))
      val todo = schema.getModelByName_!("Todo")
      todo.isEmbedded should be(false)
      val comment = schema.getModelByName_!("Comment")
      comment.isEmbedded should be(true)
      val other = schema.getModelByName_!("Other")
      other.isEmbedded should be(true)
    }

    "work with a relation from embedded to embedded type even when the relations are not named" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment]
          |}
          |
          |type Comment @embedded {
          |  test: String
          |  other: Other
          |}
          |
          |type Other @embedded{
          |  test: String
          |}""".stripMargin.trim()
      val schema = infer(emptyProject.schema, types, capabilities = ConnectorCapabilities(EmbeddedTypesCapability, RelationLinkListCapability))

      schema.relations should have(size(2))
      val relation = schema.relations.head
      relation.modelAName should equal("Comment")
      relation.modelBName should equal("Other")
      relation.template.manifestation should be(None)

      val relation2 = schema.relations.reverse.head
      relation2.modelAName should equal("Comment")
      relation2.modelBName should equal("Todo")
      relation2.template.manifestation should be(None)

      schema.models should have(size(3))
      val todo = schema.getModelByName_!("Todo")
      todo.isEmbedded should be(false)
      val comment = schema.getModelByName_!("Comment")
      comment.isEmbedded should be(true)
      val other = schema.getModelByName_!("Other")
      other.isEmbedded should be(true)

    }

  }

  //Fixme more ideas for test cases / spec work
//  schema with only embedded types
//  embedded type in other embedded type
//  non-embedded type within embedded type
//  self relations between embedded types
//  embedded types on connector without embedded types capability

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty, capabilities: ConnectorCapabilities): Schema = {
    val prismaSdl = DataModelValidatorImpl.validate(types, deployConnector.fieldRequirements, capabilities).get.dataModel
    SchemaInferrer(capabilities).infer(schema, mapping, prismaSdl, InferredTables.empty)
  }
}
