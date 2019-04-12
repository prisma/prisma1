package com.prisma.deploy.migration.inference

import com.prisma.deploy.migration.validation.DataModelValidatorImpl
import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.{ConnectorCapabilities, OnDelete, Schema}
import com.prisma.shared.schema_dsl.TestProject
import org.scalactic.{Bad, Good}
import org.scalatest.{Matchers, WordSpec}

class SchemaInfererOnDeleteSpec extends WordSpec with Matchers with DeploySpecBase {
  val emptyProject = TestProject.empty

  "Inferring onDelete relationDirectives" should {
    "work if one side provides onDelete" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name:"MyRelationName" onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  id: ID! @id
          |  todo: Todo! @relation(name:"MyRelationName")
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.SetNull)
      relation.modelBName should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.Cascade)
    }

    "work if both sides provide onDelete" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name:"MyRelationName" onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  id: ID! @id
          |  todo: Todo! @relation(name:"MyRelationName" onDelete: SET_NULL)
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.SetNull)
      relation.modelBName should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.Cascade)
    }

    "work if second side provides onDelete" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name:"MyRelationName")
          |}
          |
          |type Comment {
          |  id: ID! @id
          |  todo: Todo! @relation(name:"MyRelationName" onDelete: CASCADE)
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.Cascade)
      relation.modelBName should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.SetNull)
    }

    "handle two relations between the same models" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name:"MyRelationName")
          |  comments2: [Comment] @relation(name:"MyRelationName2" onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  id: ID! @id
          |  todo: Todo! @relation(name:"MyRelationName" onDelete: CASCADE)
          |  todo2: Todo! @relation(name:"MyRelationName2")
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(2))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.Cascade)
      relation.modelBName should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.SetNull)

      val relation2 = schema.getRelationByName_!("MyRelationName2")
      relation2.modelAName should equal("Comment")
      relation2.modelAOnDelete should equal(OnDelete.SetNull)
      relation2.modelBName should equal("Todo")
      relation2.modelBOnDelete should equal(OnDelete.Cascade)
    }

    "handle two relations between the same models 2" in {
      val types =
        """
          |type Todo {
          |  id: ID! @id
          |  comments: [Comment] @relation(name:"MyRelationName", onDelete: CASCADE)
          |  comments2: [Comment] @relation(name:"MyRelationName2", onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  id: ID! @id
          |  todo: Todo! @relation(name:"MyRelationName", onDelete: CASCADE)
          |  todo2: Todo! @relation(name:"MyRelationName2",onDelete: CASCADE)
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(2))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAName should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.Cascade)
      relation.modelBName should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.Cascade)

      val relation2 = schema.getRelationByName_!("MyRelationName2")
      relation2.modelAName should equal("Comment")
      relation2.modelAOnDelete should equal(OnDelete.Cascade)
      relation2.modelBName should equal("Todo")
      relation2.modelBOnDelete should equal(OnDelete.Cascade)
    }

    "handle relations between the three models" in {
      val types =
        """
          |type Parent {
          |  id: ID! @id
          |  child: Child! @relation(name:"ParentToChild", onDelete: CASCADE, link: INLINE)
          |  stepChild: StepChild! @relation(name:"ParentToStepChild", onDelete: CASCADE, link: INLINE)
          |}
          |
          |type Child {
          |  id: ID! @id
          |  parent: Parent! @relation(name:"ParentToChild")
          |}
          |
          |type StepChild {
          |  id: ID! @id
          |  parent: Parent! @relation(name:"ParentToStepChild")
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(2))
      val relation = schema.getRelationByName_!("ParentToChild")
      relation.modelAName should equal("Child")
      relation.modelAOnDelete should equal(OnDelete.SetNull)
      relation.modelBName should equal("Parent")
      relation.modelBOnDelete should equal(OnDelete.Cascade)

      val relation2 = schema.getRelationByName_!("ParentToStepChild")
      relation2.modelAName should equal("Parent")
      relation2.modelAOnDelete should equal(OnDelete.Cascade)
      relation2.modelBName should equal("StepChild")
      relation2.modelBOnDelete should equal(OnDelete.SetNull)
    }
  }

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty): Schema = {
    val capabilities = ConnectorCapabilities()
    val inferer      = SchemaInferrer(capabilities)

    val validator = DataModelValidatorImpl(
      types,
      capabilities
    )

    val dataModelAst = validator.validate match {
      case Good(validationResult) =>
        validationResult.dataModel
      case Bad(errors) =>
        sys.error(s"The validation of the Datamodel returned errors: ${errors.mkString("\n")}")
    }

    inferer.infer(schema, SchemaMapping.empty, dataModelAst)
  }
}
