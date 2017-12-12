package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class SchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaAssertions {

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
}
