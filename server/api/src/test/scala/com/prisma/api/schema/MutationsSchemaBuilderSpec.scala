package com.prisma.api.schema

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class MutationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the create Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("tag", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("createTodo(data: TodoCreateInput!): Todo!")
    schema should containInputType("TodoCreateInput", fields = Vector("title: String!", "tag: String"))
  }

  "the create Mutation for a model with relations" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      schema
        .model("Todo")
        .field_!("title", _.String)
        .field("tag", _.String)
        .oneToManyRelation("comments", "todo", comment)
        .oneToOneRelation_!("topComment", "topCommentFor", comment)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    // from Todo to Comment
    schema should containMutation("createTodo(data: TodoCreateInput!): Todo!")
    schema should containInputType(
      "TodoCreateInput",
      fields = Vector(
        "title: String!",
        "tag: String",
        "comments: CommentCreateManyWithoutTodoInput",
        "topComment: CommentCreateOneWithoutTodoInput!"
      )
    )

    schema should containInputType("CommentCreateManyWithoutTodoInput",
                                   fields = Vector(
                                     "create: [CommentCreateWithoutTodoInput!]",
                                     "connect: [CommentWhereUniqueInput!]"
                                   ))

    schema should containInputType("CommentCreateWithoutTodoInput", fields = Vector("text: String!"))

    // from Comment to Todo
    schema should containInputType("CommentCreateInput",
                                   fields = Vector(
                                     "text: String!",
                                     "todo: TodoCreateOneWithoutCommentsInput"
                                   ))

    schema should containInputType("TodoCreateOneWithoutCommentsInput",
                                   fields = Vector(
                                     "create: TodoCreateWithoutCommentsInput",
                                     "connect: TodoWhereUniqueInput"
                                   ))

    schema should containInputType("TodoCreateWithoutCommentsInput",
                                   fields = Vector(
                                     "title: String!",
                                     "tag: String"
                                   ))
  }

  "the update Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    schema should containInputType("TodoUpdateInput",
                                   fields = Vector(
                                     "title: String",
                                     "alias: String"
                                   ))

    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID",
                                     "alias: String"
                                   ))
  }

  "the update Mutation for a model with a optional backrelation" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("listUnique", _.String, isUnique = true).field("optList", _.String)
      val todo = schema.model("Todo").field_!("todoUnique", _.String, isUnique = true).field("optString", _.String)
      list.manyToManyRelation("todoes", "does not matter", todo, includeFieldB = false)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    schema should containInputType("TodoCreateInput",
                                   fields = Vector(
                                     "todoUnique: String!",
                                     "optString: String"
                                   ))

    schema should containInputType("TodoUpdateInput",
                                   fields = Vector(
                                     "todoUnique: String",
                                     "optString: String"
                                   ))

    schema should containInputType("TodoUpdateDataInput",
                                   fields = Vector(
                                     "todoUnique: String",
                                     "optString: String"
                                   ))

    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID",
                                     "todoUnique: String"
                                   ))

    schema should containInputType("TodoUpdateNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "data: TodoUpdateDataInput!"
                                   ))

    schema should containInputType("TodoUpsertNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "update: TodoUpdateDataInput!",
                                     "create: TodoCreateInput!"
                                   ))
  }

  "the update many Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("updateManyTodoes(data: TodoUpdateInput!, where: TodoWhereInput!): BatchPayload!")
    schema should containInputType("TodoWhereInput")
  }

  "the many update Mutation for a model" should "not be generated for an empty model" in {
    val project = SchemaDsl() { schema =>
      val model = schema.model("Todo")
      model.fields.clear()
      model.field_!("id", _.GraphQLID, isHidden = true)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema shouldNot containMutation("updateManyTodoes(data: TodoUpdateInput!, where: TodoWhereInput!): BatchPayload!")
    schema should containInputType("TodoWhereInput",
                                   fields = Vector(
                                     "AND: [TodoWhereInput!]",
                                     "OR: [TodoWhereInput!]"
                                   ))
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
    schema should containMutation("updateTodo(data: TodoUpdateInput!, where: TodoWhereUniqueInput!): Todo")

    schema should containInputType("TodoUpdateInput",
                                   fields = Vector(
                                     "title: String",
                                     "tag: String",
                                     "comments: CommentUpdateManyWithoutTodoInput"
                                   ))

    schema should containInputType(
      "CommentUpdateManyWithoutTodoInput",
      fields = Vector(
        "create: [CommentCreateWithoutTodoInput!]",
        "connect: [CommentWhereUniqueInput!]",
        "disconnect: [CommentWhereUniqueInput!]",
        "delete: [CommentWhereUniqueInput!]",
        "update: [CommentUpdateWithoutTodoInput!]",
        "upsert: [CommentUpsertWithoutTodoInput!]"
      )
    )

    schema should containInputType("CommentCreateWithoutTodoInput",
                                   fields = Vector(
                                     "text: String!"
                                   ))

    schema should containInputType("CommentUpdateWithoutTodoInput",
                                   fields = Vector(
                                     "where: CommentWhereUniqueInput!",
                                     "data: CommentUpdateWithoutTodoDataInput!"
                                   ))

    schema should containInputType("CommentUpdateWithoutTodoDataInput",
                                   fields = Vector(
                                     "text: String"
                                   ))

    schema should containInputType(
      "CommentUpsertWithoutTodoInput",
      fields = Vector(
        "where: CommentWhereUniqueInput!",
        "update: CommentUpdateWithoutTodoDataInput!",
        "create: CommentCreateWithoutTodoInput!"
      )
    )

    // from Comment to Todo
    schema should containInputType("CommentUpdateInput",
                                   fields = Vector(
                                     "text: String",
                                     "todo: TodoUpdateOneWithoutCommentsInput"
                                   ))

    schema should containInputType(
      "TodoUpdateOneWithoutCommentsInput",
      fields = Vector(
        "create: TodoCreateWithoutCommentsInput",
        "connect: TodoWhereUniqueInput",
        "disconnect: TodoWhereUniqueInput",
        "delete: TodoWhereUniqueInput",
        "update: TodoUpdateWithoutCommentsInput",
        "upsert: TodoUpsertWithoutCommentsInput"
      )
    )

    schema should containInputType("TodoCreateWithoutCommentsInput",
                                   fields = Vector(
                                     "title: String!",
                                     "tag: String"
                                   ))

    schema should containInputType("TodoUpdateWithoutCommentsInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "data: TodoUpdateWithoutCommentsDataInput!"
                                   ))

    schema should containInputType("TodoUpdateWithoutCommentsDataInput",
                                   fields = Vector(
                                     "title: String",
                                     "tag: String"
                                   ))

    schema should containInputType(
      "TodoUpsertWithoutCommentsInput",
      fields = Vector(
        "where: TodoWhereUniqueInput!",
        "update: TodoUpdateWithoutCommentsDataInput!",
        "create: TodoCreateWithoutCommentsInput!"
      )
    )
  }

  "the update and upsert Mutation for a model with omitted back relation" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment").field_!("text", _.String)
      val todo    = schema.model("Todo").field_!("title", _.String).field("tag", _.String)
      todo.oneToManyRelation("comments", "todo", comment, includeFieldB = false)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containInputType(
      "CommentUpdateManyInput",
      fields = Vector(
        "create: [CommentCreateInput!]",
        "connect: [CommentWhereUniqueInput!]",
        "disconnect: [CommentWhereUniqueInput!]",
        "delete: [CommentWhereUniqueInput!]",
        "update: [CommentUpdateNestedInput!]",
        "upsert: [CommentUpsertNestedInput!]"
      )
    )
  }

  "the upsert Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String)
    }
    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("upsertTodo(where: TodoWhereUniqueInput!, create: TodoCreateInput!, update: TodoUpdateInput!): Todo!")
  }

  "the delete Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field_!("title", _.String).field("tag", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("deleteTodo(where: TodoWhereUniqueInput!): Todo")
    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID"
                                   ))
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

    schema should containMutation("deleteTodo(where: TodoWhereUniqueInput!): Todo")
    schema should containInputType("TodoWhereUniqueInput",
                                   fields = Vector(
                                     "id: ID",
                                     "unique: Int"
                                   ))
  }

  "the delete many Mutation for a model" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      schema
        .model("Todo")
        .field_!("title", _.String)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should containMutation("deleteManyTodoes(where: TodoWhereInput!): BatchPayload!")
    schema should containInputType("TodoWhereInput")
  }
}
