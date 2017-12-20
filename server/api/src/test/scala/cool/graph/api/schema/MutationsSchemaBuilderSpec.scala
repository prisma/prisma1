package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class MutationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaAssertions {

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

  "the create Mutation for a model with relations" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .oneToManyRelation("comments", "todo", comment)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    // from Todo to Comment
    val mutation = schema.mustContainMutation("createTodo")
    mustBeEqual(mutation, "createTodo(data: TodoCreateInput!): Todo!")

    val todoInputType = schema.mustContainInputType("TodoCreateInput")
    mustBeEqual(
      todoInputType,
      """input TodoCreateInput {
        |  title: String!
        |  tag: String
        |  comments: CommentCreateManyWithoutTodoInput
        |}""".stripMargin
    )

    val nestedInputTypeForComment = schema.mustContainInputType("CommentCreateManyWithoutTodoInput")

    mustBeEqual(
      nestedInputTypeForComment,
      """input CommentCreateManyWithoutTodoInput {
         |  create: [CommentCreateWithoutTodoInput!]
         |  connect: [CommentWhereUniqueInput!]
         |}""".stripMargin
    )

    val createInputForNestedComment = schema.mustContainInputType("CommentCreateWithoutTodoInput")
    mustBeEqual(
      createInputForNestedComment,
      """input CommentCreateWithoutTodoInput {
        |  text: String!
        |}""".stripMargin
    )

    // from Comment to Todo
    val commentInputType = schema.mustContainInputType("CommentCreateInput")
    mustBeEqual(
      commentInputType,
      """input CommentCreateInput {
       |  text: String!
       |  todo: TodoCreateOneWithoutCommentsInput
       |}""".stripMargin
    )

    val nestedInputTypeForTodo = schema.mustContainInputType("TodoCreateOneWithoutCommentsInput")
    mustBeEqual(
      nestedInputTypeForTodo,
      """input TodoCreateOneWithoutCommentsInput {
       |  create: TodoCreateWithoutCommentsInput
       |  connect: TodoWhereUniqueInput
       |}""".stripMargin
    )

    val createInputForNestedTodo = schema.mustContainInputType("TodoCreateWithoutCommentsInput")
    mustBeEqual(
      createInputForNestedTodo,
      """input TodoCreateWithoutCommentsInput {
        |  title: String!
        |  tag: String
        |}""".stripMargin
    )
  }

  "the update Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("updateTodo")
    mutation should be("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    val inputType = schema.mustContainInputType("TodoUpdateInput")
    inputType should be("""input TodoUpdateInput {
                          |  title: String
                          |  alias: String
                          |}""".stripMargin)

    val whereInputType = schema.mustContainInputType("TodoWhereUniqueInput")
    whereInputType should be("""input TodoWhereUniqueInput {
                                |  id: ID
                                |  alias: String
                                |}""".stripMargin)
  }

  "the update many Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("updateTodoes")
    mustBeEqual(mutation, "updateTodoes(data: TodoUpdateInput!, where: TodoWhereInput!): BatchPayload!")

    schema.mustContainInputType("TodoWhereInput")
  }

  "the multi update Mutation for a model" should "be generated correctly for an empty model" in {
    val project = SchemaDsl() { schema =>
      val model = schema.model("Todo")
      model.fields.clear()
      model.field_!("id", _.GraphQLID, isHidden = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("updateTodoes")
    mustBeEqual(mutation, "updateTodoes(data: TodoUpdateInput!, where: TodoWhereInput!): BatchPayload!")

    mustBeEqual(
      schema.mustContainInputType("TodoWhereInput"),
      """input TodoWhereInput {
        |  # Logical AND on all given filters.
        |  AND: [TodoWhereInput!]
        |
        |  # Logical OR on all given filters.
        |  OR: [TodoWhereInput!]
        |}"""
    )
  }

  "the update Mutation for a model with relations" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .oneToManyRelation("comments", "todo", comment)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    // from Todo to Comment
    val mutation = schema.mustContainMutation("updateTodo")
    mustBeEqual(mutation, "updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    val todoInputType = schema.mustContainInputType("TodoUpdateInput")
    mustBeEqual(
      todoInputType,
      """input TodoUpdateInput {
        |  title: String
        |  tag: String
        |  comments: CommentUpdateManyWithoutTodoInput
        |}""".stripMargin
    )

    val nestedInputTypeForComment = schema.mustContainInputType("CommentUpdateManyWithoutTodoInput")
    mustBeEqual(
      nestedInputTypeForComment,
      """input CommentUpdateManyWithoutTodoInput {
        |  create: [CommentCreateWithoutTodoInput!]
        |  connect: [CommentWhereUniqueInput!]
        |  disconnect: [CommentWhereUniqueInput!]
        |  delete: [CommentWhereUniqueInput!]
        |  update: [CommentUpdateWithoutTodoInput!]
        |  upsert: [CommentUpsertWithoutTodoInput!]
        |}""".stripMargin
    )

    val createInputForNestedComment = schema.mustContainInputType("CommentCreateWithoutTodoInput")
    mustBeEqual(
      createInputForNestedComment,
      """input CommentCreateWithoutTodoInput {
        |  text: String!
        |}""".stripMargin
    )

    val updateInputForNestedComment = schema.mustContainInputType("CommentUpdateWithoutTodoInput")
    mustBeEqual(
      updateInputForNestedComment,
      """input CommentUpdateWithoutTodoInput {
        |  where: CommentWhereUniqueInput!
        |  data: CommentUpdateWithoutTodoDataInput!
        |}""".stripMargin
    )

    val updateDataInputForNestedComment = schema.mustContainInputType("CommentUpdateWithoutTodoDataInput")
    mustBeEqual(
      updateDataInputForNestedComment,
      """input CommentUpdateWithoutTodoDataInput {
        |  text: String
        |}""".stripMargin
    )

    val upsertDataInputForNestedComment = schema.mustContainInputType("CommentUpsertWithoutTodoInput")
    mustBeEqual(
      upsertDataInputForNestedComment,
      """input CommentUpsertWithoutTodoInput {
        |  where: CommentWhereUniqueInput!
        |  update: CommentUpdateWithoutTodoDataInput!
        |  create: CommentCreateWithoutTodoInput!
        |}""".stripMargin
    )

    // from Comment to Todo
    val commentInputType = schema.mustContainInputType("CommentUpdateInput")
    mustBeEqual(
      commentInputType,
      """input CommentUpdateInput {
        |  text: String
        |  todo: TodoUpdateOneWithoutCommentsInput
        |}""".stripMargin
    )

    val nestedInputTypeForTodo = schema.mustContainInputType("TodoUpdateOneWithoutCommentsInput")
    mustBeEqual(
      nestedInputTypeForTodo,
      """input TodoUpdateOneWithoutCommentsInput {
        |  create: TodoCreateWithoutCommentsInput
        |  connect: TodoWhereUniqueInput
        |  disconnect: TodoWhereUniqueInput
        |  delete: TodoWhereUniqueInput
        |  update: TodoUpdateWithoutCommentsInput
        |  upsert: TodoUpsertWithoutCommentsInput
        |}""".stripMargin
    )

    val createInputForNestedTodo = schema.mustContainInputType("TodoCreateWithoutCommentsInput")
    mustBeEqual(
      createInputForNestedTodo,
      """input TodoCreateWithoutCommentsInput {
        |  title: String!
        |  tag: String
        |}""".stripMargin
    )

    val updateInputForNestedTodo = schema.mustContainInputType("TodoUpdateWithoutCommentsInput")
    mustBeEqual(
      updateInputForNestedTodo,
      """input TodoUpdateWithoutCommentsInput {
        |  where: TodoWhereUniqueInput!
        |  data: TodoUpdateWithoutCommentsDataInput!
        |}""".stripMargin
    )

    val updateDataInputForNestedTodo = schema.mustContainInputType("TodoUpdateWithoutCommentsDataInput")
    mustBeEqual(
      updateDataInputForNestedTodo,
      """input TodoUpdateWithoutCommentsDataInput {
        |  title: String
        |  tag: String
        |}""".stripMargin
    )

    val upsertDataInputForNestedTodo = schema.mustContainInputType("TodoUpsertWithoutCommentsInput")
    mustBeEqual(
      upsertDataInputForNestedTodo,
      """input TodoUpsertWithoutCommentsInput {
        |  where: TodoWhereUniqueInput!
        |  update: TodoUpdateWithoutCommentsDataInput!
        |  create: TodoCreateWithoutCommentsInput!
        |}""".stripMargin
    )
  }

  "the upsert Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("upsertTodo")
    mustBeEqual(
      mutation,
      "upsertTodo(where: TodoWhereUniqueInput!, create: TodoCreateInput!, update: TodoUpdateInput!): Todo!"
    )
  }

  "the delete Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("tag", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("deleteTodo")
    mutation should be("deleteTodo(where: TodoWhereUniqueInput!): Todo")

    val inputType = schema.mustContainInputType("TodoWhereUniqueInput")
    inputType should be("""input TodoWhereUniqueInput {
                          |  id: ID
                          |}""".stripMargin)
  }

  "the delete Mutation for a model" should "be generated correctly and contain all non-list unique fields" in {
    val project = SchemaDsl() { schema =>
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .field("unique", _.Int, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("deleteTodo")
    mutation should be("deleteTodo(where: TodoWhereUniqueInput!): Todo")

    val inputType = schema.mustContainInputType("TodoWhereUniqueInput")
    inputType should be("""input TodoWhereUniqueInput {
                          |  id: ID
                          |  unique: Int
                          |}""".stripMargin)
  }

  "the delete many Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema
        .model("Todo")
        .field_!("title", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    val mutation = schema.mustContainMutation("deleteTodoes")
    mustBeEqual(mutation, "deleteTodoes(where: TodoWhereInput!): BatchPayload!")

    schema.mustContainInputType("TodoWhereInput")
  }
}
