package com.prisma.deploy.migration.inference

import com.prisma.shared.models.{OnDelete, Schema}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalactic.Or
import org.scalatest.{Matchers, WordSpec}
import sangria.parser.QueryParser

class SchemaInfererSpec extends WordSpec with Matchers {

  val inferrer     = SchemaInferrer()
  val emptyProject = SchemaDsl().buildProject()

  "if a given relation does not exist yet, the inferer" should {
    "infer relations with the given name if a relation directive is provided on both sides" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyNameForTodoToComments")
          |}
          |
          |type Comment {
          |  todo: Todo! @relation(name:"MyNameForTodoToComments")
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get

      val relation = schema.getRelationByName_!("MyNameForTodoToComments")
      relation.modelAId should equal("Comment")
      relation.modelBId should equal("Todo")
    }

    "infer relations with the given name if a relation directive is provided on both sides and there are two relations between models" in {
      val types =
        """
          |type User {
          |  calls: [Call!]! @relation(name: "CallRequester")
          |  calls_member: [Call!]! @relation(name: "CallMembers")
          |}
          |type Call {
          |  created_by: User! @relation(name: "CallRequester")
          |  members: [User!]! @relation(name: "CallMembers")
          |}""".stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get

      val relation = schema.getRelationByName_!("CallRequester")
      relation.modelAId should equal("Call")
      relation.modelBId should equal("User")

      val relation2 = schema.getRelationByName_!("CallMembers")
      relation2.modelAId should equal("Call")
      relation2.modelBId should equal("User")
    }

    "infer relations with provided name if only one relation directive is given" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyRelationName")
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
      relation.modelBId should equal("Todo")
    }

    "infer relations with an auto generated name if no relation directive is given" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!]
          |}
          |
          |type Comment {
          |  todo: Todo!
          |}
        """.stripMargin.trim()
      val schema = infer(emptyProject.schema, types).get
      schema.relations.foreach(println(_))

      val relation = schema.getRelationByName_!("CommentToTodo")
      relation.modelAId should equal("Comment")
      relation.modelBId should equal("Todo")

      val field1 = schema.getModelByName_!("Todo").getFieldByName_!("comments")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = schema.getModelByName_!("Comment").getFieldByName_!("todo")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }
  }

  "if a given relation does already exist, the inferer" should {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment")
      schema.model("Todo").oneToManyRelation("comments", "todo", comment, relationName = Some("CommentToTodo"))
    }

    "infer the existing relation and update it accordingly when the type names change" in {
      val types =
        """
          |type TodoNew {
          |  comments: [CommentNew!]
          |}
          |
          |type CommentNew {
          |  todo: TodoNew!
          |}
        """.stripMargin

      val renames = SchemaMapping(
        models = Vector(
          Mapping(previous = "Todo", next = "TodoNew"),
          Mapping(previous = "Comment", next = "CommentNew")
        )
      )

      val newSchema = infer(project.schema, types, renames).get
      newSchema.relations.foreach(println(_))

      val relation = newSchema.getRelationByName_!("CommentNewToTodoNew")
      relation.modelAId should be("TodoNew")
      relation.modelBId should be("CommentNew")

      val field1 = newSchema.getModelByName_!("TodoNew").getFieldByName_!("comments")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = newSchema.getModelByName_!("CommentNew").getFieldByName_!("todo")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }

    "infer the existing relation although the type and field names changed" in {
      val types =
        """
          |type TodoNew {
          |  commentsNew: [CommentNew!]
          |}
          |
          |type CommentNew {
          |  todoNew: TodoNew!
          |}
        """.stripMargin

      val renames = SchemaMapping(
        models = Vector(
          Mapping(previous = "Todo", next = "TodoNew"),
          Mapping(previous = "Comment", next = "CommentNew")
        ),
        fields = Vector(
          FieldMapping(previousModel = "Todo", previousField = "comments", nextModel = "TodoNew", nextField = "commentsNew"),
          FieldMapping(previousModel = "Comment", previousField = "todo", nextModel = "CommentNew", nextField = "todoNew")
        )
      )

      val newSchema = infer(project.schema, types, renames).get
      newSchema.relations.foreach(println(_))

      val relation = newSchema.getRelationByName_!("CommentNewToTodoNew")
      relation.modelAId should be("TodoNew")
      relation.modelBId should be("CommentNew")

      val field1 = newSchema.getModelByName_!("TodoNew").getFieldByName_!("commentsNew")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = newSchema.getModelByName_!("CommentNew").getFieldByName_!("todoNew")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }
  }

  "if a model already exists and it gets renamed, the inferrer" should {
    "infer the next model with the stable identifier of the existing model" in {
      val project = SchemaDsl() { schema =>
        schema.model("Todo").field("title", _.String)
      }
      val types =
        """
          |type TodoNew {
          |  title: String
          |}
        """.stripMargin

      val renames = SchemaMapping(
        models = Vector(
          Mapping(previous = "Todo", next = "TodoNew")
        )
      )

      val newSchema = infer(project.schema, types, renames).get

      val previousModel = project.schema.getModelByName_!("Todo")
      val nextModel     = newSchema.getModelByName_!("TodoNew")

      previousModel.stableIdentifier should equal(nextModel.stableIdentifier)
    }
  }

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty): Or[Schema, ProjectSyntaxError] = {
    val document = QueryParser.parse(types).get
    inferrer.infer(schema, mapping, document)
  }
}
