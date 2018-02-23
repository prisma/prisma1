package com.prisma.api.schema

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class OneRelationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the update Mutation for a many to many relation with a optional backrelation" should "be generated correctly" in {
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

  "the update Mutation for a one to one relation with a optional backrelation" should "be generated correctly" in {
    val project = SchemaDsl() { schema =>
      val list = schema.model("List").field_!("listUnique", _.String, isUnique = true).field("optList", _.String)
      val todo = schema.model("Todo").field_!("todoUnique", _.String, isUnique = true).field("optString", _.String)
      list.oneToOneRelation("todo", "does not matter", todo, includeFieldB = false)
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    println(schema)

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

    schema should containInputType(
      "TodoUpdateOneInput",
      fields = Vector(
        "create: TodoCreateInput",
        "connect: TodoWhereUniqueInput",
        "disconnect: TodoWhereUniqueInput",
        "delete: TodoWhereUniqueInput",
        "update: TodoUpdateDataInput",
        "upsert: TodoUpsertNestedInput"
      )
    )

    schema should containInputType("TodoUpsertNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "update: TodoUpdateDataInput!",
                                     "create: TodoCreateInput!"
                                   ))
  }

}
