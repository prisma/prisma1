package com.prisma.deploy.migration.inference

import com.prisma.deploy.connector.InferredTables
import com.prisma.deploy.migration.validation.SchemaSyntaxValidator
import com.prisma.shared.models.Manifestations.{FieldManifestation, InlineRelationManifestation, ModelManifestation, RelationTableManifestation}
import com.prisma.shared.models.{RelationSide, Schema}
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{Matchers, WordSpec}

class SchemaInfererSpec extends WordSpec with Matchers {
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
      val schema = infer(emptyProject.schema, types)

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
      val schema = infer(emptyProject.schema, types)

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
      val schema = infer(emptyProject.schema, types)

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
      val schema = infer(emptyProject.schema, types)
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

      val newSchema = infer(project.schema, types, renames)
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

      val newSchema = infer(project.schema, types, renames)
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

      val newSchema = infer(project.schema, types, renames)

      val previousModel = project.schema.getModelByName_!("Todo")
      val nextModel     = newSchema.getModelByName_!("TodoNew")

      previousModel.stableIdentifier should equal(nextModel.stableIdentifier)
    }
  }

  "For self-relations the inferer" should {
    "assign fieldA to the field with the lower lexicographic order" in {
      val types =
        """|type Technology {
           |  name: String! @unique
           |  childTechnologies: [Technology!]! @relation(name: "ChildTechnologies")
           |  parentTechnologies: [Technology!]! @relation(name: "ChildTechnologies")
           |}""".stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("ChildTechnologies")
      relation.modelAId should equal("Technology")
      relation.modelBId should equal("Technology")
      relation.modelAField.get.name should be("childTechnologies")
      relation.modelBField.get.name should be("parentTechnologies")

    }

    "keep assignments after renames" in {
      val types =
        """|type Technology {
           |  name: String! @unique
           |  childTechnologies: [Technology!]! @relation(name: "ChildTechnologies")
           |  parentTechnologies: [Technology!]! @relation(name: "ChildTechnologies")
           |}""".stripMargin.trim()
      val schema = infer(emptyProject.schema, types)

      schema.relations should have(size(1))
      val relation = schema.getRelationByName_!("ChildTechnologies")
      relation.modelAId should equal("Technology")
      relation.modelBId should equal("Technology")
      relation.modelAField.get.name should be("childTechnologies")
      relation.modelBField.get.name should be("parentTechnologies")

      val newTypes =
        """|type NewTechnology {
           |  name: String! @unique
           |  xTechnologies: [NewTechnology!]! @relation(name: "ChildTechnologies")
           |  parentTechnologies: [NewTechnology!]! @relation(name: "ChildTechnologies")
           |}""".stripMargin.trim()

      val renames = SchemaMapping(
        models = Vector(Mapping(previous = "Technology", next = "NewTechnology")),
        fields =
          Vector(FieldMapping(previousModel = "Technology", previousField = "childTechnologies", nextModel = "NewTechnology", nextField = "xTechnologies"))
      )

      val newSchema = infer(schema, newTypes, renames)
      newSchema.relations.foreach(println(_))

      val newRelation = newSchema.getRelationByName_!("ChildTechnologies")
      newRelation.modelAId should be("NewTechnology")
      newRelation.modelBId should be("NewTechnology")

      val field1 = newSchema.getModelByName_!("NewTechnology").getFieldByName_!("xTechnologies")
      field1.relation should be(Some(newRelation))
      field1.relationSide.get.toString should be("A")

      val field2 = newSchema.getModelByName_!("NewTechnology").getFieldByName_!("parentTechnologies")
      field2.relation should be(Some(newRelation))
      field2.relationSide.get.toString should be("B")
    }
  }

  "repair invalid assignments" in {
    val types =
      """|type Technology {
         |  name: String! @unique
         |  childTechnologies: [Technology!]! @relation(name: "ChildTechnologies")
         |  parentTechnologies: [Technology!]! @relation(name: "ChildTechnologies")
         |}""".stripMargin.trim()
    val schema = infer(emptyProject.schema, types)

    schema.relations should have(size(1))
    val relation = schema.getRelationByName_!("ChildTechnologies")
    relation.modelAId should equal("Technology")
    relation.modelBId should equal("Technology")
    relation.modelAField.get.name should be("childTechnologies")
    relation.modelBField.get.name should be("parentTechnologies")

    val techModel   = schema.models.head
    val parentField = techModel.getFieldByName_!("parentTechnologies")

    val updatedModel =
      techModel.copy(fieldTemplates = techModel.fields.filter(_ != parentField).map(_.copy()) :+ parentField.copy(relationSide = Some(RelationSide.A)))
    val invalidSchema = schema.copy(modelTemplates = List(updatedModel))

    val newSchema = infer(invalidSchema, types)
    newSchema.relations.foreach(println(_))

    val newRelation = newSchema.getRelationByName_!("ChildTechnologies")
    newRelation.modelAId should be("Technology")
    newRelation.modelBId should be("Technology")

    val field1 = newSchema.getModelByName_!("Technology").getFieldByName_!("childTechnologies")
    field1.relation should be(Some(newRelation))
    field1.relationSide.get.toString should be("A")

    val field2 = newSchema.getModelByName_!("Technology").getFieldByName_!("parentTechnologies")
    field2.relation should be(Some(newRelation))
    field2.relationSide.get.toString should be("B")
  }

  "handle optional backrelations" in {
    val types =
      """|type Technology {
         |  name: String! @unique
         |  childTechnologies: [Technology!]!
         |}""".stripMargin.trim()
    val schema = infer(emptyProject.schema, types)

    schema.relations should have(size(1))
    val relation = schema.relations.head
    relation.modelAId should equal("Technology")
    relation.modelBId should equal("Technology")
    relation.modelAField.get.name should be("childTechnologies")
  }

  "handle database manifestations for models" in {
    val types =
      """|type Todo @pgTable(name:"todo_table"){
         |  name: String!
         |}""".stripMargin
    val schema = infer(emptyProject.schema, types)

    val model = schema.getModelByName_!("Todo")
    model.manifestation should equal(Some(ModelManifestation("todo_table")))
  }

  "handle database manifestations for fields" in {
    val types =
      """|type Todo {
         |  name: String! @pgColumn(name: "my_name_column")
         |}""".stripMargin
    val schema = infer(emptyProject.schema, types)

    val field = schema.getModelByName_!("Todo").getFieldByName_!("name")
    field.manifestation should equal(Some(FieldManifestation("my_name_column")))
  }

  "handle relation table manifestations" ignore {
    val types =
      """|type Todo {
         |  name: String!
         |}
         |
         |type List {
         |  todos: [Todo] @relationTable(table: "list_to_todo", relationColumn: "list_id", targetColumn: "todo_id")
         |}""".stripMargin
    val schema = infer(emptyProject.schema, types, isActive = false)

    val relation = schema.getModelByName_!("List").getFieldByName_!("todos").relation.get
    // assert model ids to make sure that the generated manifestation refers to the right modelAColumn/modelBColumn
    relation.modelAId should equal("List")
    relation.modelBId should equal("Todo")

    val expectedManifestation = RelationTableManifestation(table = "list_to_todo", modelAColumn = "list_id", modelBColumn = "todo_id")
    relation.manifestation should equal(Some(expectedManifestation))
  }

  "handle inline relation manifestations" ignore {
    val types =
      """
         |type List {
         |  todos: [Todo]
         |}
         |
         |type Todo {
         |  name: String!
         |  list: List @pgRelation(column: "list_id")
         |}
         |""".stripMargin
    val schema = infer(emptyProject.schema, types, isActive = false)

    val relation = schema.getModelByName_!("List").getFieldByName_!("todos").relation.get

    val expectedManifestation = InlineRelationManifestation(inTableOfModelId = "Todo", referencingColumn = "list_id")
    relation.manifestation should equal(Some(expectedManifestation))
  }

  "add hidden reserved fields if isActive is true" in {
    val types =
      """|type Todo {
         |  name: String!
         |}""".stripMargin
    val schema = infer(emptyProject.schema, types, isActive = true)

    val model = schema.getModelByName_!("Todo")
    model.fields should have(size(4))
    model.fields.map(_.name) should equal(List("name", "id", "updatedAt", "createdAt"))
  }

  "do not add hidden reserved fields if isActive is false" in {
    val types =
      """|type Todo {
         |  name: String!
         |}""".stripMargin
    val schema = infer(emptyProject.schema, types, isActive = false)

    val model = schema.getModelByName_!("Todo")
    model.fields should have(size(1))
    model.fields.map(_.name) should equal(List("name"))
  }

  def infer(schema: Schema, types: String, mapping: SchemaMapping = SchemaMapping.empty, isActive: Boolean = true): Schema = {
    val validator = SchemaSyntaxValidator(
      types,
      SchemaSyntaxValidator.directiveRequirements,
      SchemaSyntaxValidator.reservedFieldsRequirementsForAllConnectors,
      SchemaSyntaxValidator.requiredReservedFields,
      allowScalarLists = false
    )

    val prismaSdl = validator.generateSDL

    SchemaInferrer(isActive).infer(schema, mapping, prismaSdl, InferredTables.empty)

  }
}
