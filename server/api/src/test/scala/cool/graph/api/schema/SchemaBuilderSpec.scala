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

    val mutation = schema.mustContainMutation("createTodo")
    mutation should be("createTodo(data: TodoCreateInput!): Todo!")

    val todoInputType = schema.mustContainInputType("TodoCreateInput")
    todoInputType should be("""input TodoCreateInput {
                          |  title: String!
                          |  tag: String
                          |  comments: CommentCreateManyWithoutTodoInput
                          |}""".stripMargin)

    val nestedInputTypeForComment = schema.mustContainInputType("CommentCreateManyWithoutTodoInput")

    mustBeEqual(
      nestedInputTypeForComment,
      """input CommentCreateManyWithoutTodoInput {
         |  create: [CommentCreateWithoutTodoInput!]
         |}""".stripMargin
    )

    val createInputForNestedComment = schema.mustContainInputType("CommentCreateWithoutTodoInput")
    createInputForNestedComment should equal("""input CommentCreateWithoutTodoInput {
                                               |  text: String!
                                               |}""".stripMargin)

    val commentInputType = schema.mustContainInputType("CommentCreateInput")
    commentInputType should equal("""input CommentCreateInput {
                                    |  text: String!
                                    |  todo: TodoCreateOneWithoutCommentsInput
                                    |}""".stripMargin)

    val nestedInputTypeForTodo = schema.mustContainInputType("TodoCreateOneWithoutCommentsInput")
    nestedInputTypeForTodo should equal("""input TodoCreateOneWithoutCommentsInput {
                                          |  create: TodoCreateWithoutCommentsInput
                                          |}""".stripMargin)

    val createInputForNestedTodo = schema.mustContainInputType("TodoCreateWithoutCommentsInput")
    createInputForNestedTodo should equal("""input TodoCreateWithoutCommentsInput {
                                            |  title: String!
                                            |  tag: String
                                            |}""".stripMargin)
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

  private def mustBeEqual(actual: String, expected: String): Unit = {
    if (actual != expected) {
      sys.error(s"""The strings were not equal!
          |actual:   $actual
          |expected: $expected
        """.stripMargin)
    }
  }
}
