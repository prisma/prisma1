package com.prisma.deploy.migration.inference

import com.prisma.shared.models.{OnDelete, Schema}
import com.prisma.shared.project_dsl.SchemaDsl
import org.scalactic.Or
import org.scalatest.{Matchers, WordSpec}
import sangria.parser.QueryParser

class SchemaInfererOnDeleteSpec extends WordSpec with Matchers {

  val inferer      = SchemaInferrer()
  val emptyProject = SchemaDsl().buildProject()

  "Inferring onDelete relationDirectives" should {
    "work if one side provides onDelete" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyRelationName" onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  todo: Todo!
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAId should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.SetNull)
      relation.modelBId should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.Cascade)
    }

    "work if both sides provide onDelete" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyRelationName" onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  todo: Todo! @relation(name:"MyRelationName" onDelete: SET_NULL)
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAId should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.SetNull)
      relation.modelBId should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.Cascade)
    }

    "work if second side provides onDelete" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyRelationName")
          |}
          |
          |type Comment {
          |  todo: Todo! @relation(name:"MyRelationName" onDelete: CASCADE)
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAId should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.Cascade)
      relation.modelBId should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.SetNull)
    }

    "handle two relations between the same models" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyRelationName")
          |  comments2: [Comment!] @relation(name:"MyRelationName2" onDelete: CASCADE)
          |}
          |
          |type Comment {
          |  todo: Todo! @relation(name:"MyRelationName" onDelete: CASCADE)
          |  todo2: Todo! @relation(name:"MyRelationName2")
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get

      schema.relations should have(size(2))
      val relation = schema.getRelationByName_!("MyRelationName")
      relation.modelAId should equal("Comment")
      relation.modelAOnDelete should equal(OnDelete.Cascade)
      relation.modelBId should equal("Todo")
      relation.modelBOnDelete should equal(OnDelete.SetNull)

      val relation2 = schema.getRelationByName_!("MyRelationName2")
      relation2.modelAId should equal("Comment")
      relation2.modelAOnDelete should equal(OnDelete.SetNull)
      relation2.modelBId should equal("Todo")
      relation2.modelBOnDelete should equal(OnDelete.Cascade)
    }
  }

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty): Or[Schema, ProjectSyntaxError] = {
    val document = QueryParser.parse(types).get
    inferer.infer(schema, mapping, document)
  }
}
