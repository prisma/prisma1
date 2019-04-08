package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.Field
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class RelationsSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the update Mutation for a many to many relation with an optional backrelation" should "be generated correctly" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val list = schema.model("List").field_!("listUnique", _.String, isUnique = true).field("optList", _.String)
      val todo = schema.model("Todo").field_!("todoUnique", _.String, isUnique = true).field("optString", _.String)
      list.manyToManyRelation("todoes", "does not matter", todo, includeFieldBInSchema = false)
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

    schema should containInputType("TodoUpdateWithWhereUniqueNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "data: TodoUpdateDataInput!"
                                   ))

    schema should containInputType("TodoUpsertWithWhereUniqueNestedInput",
                                   fields = Vector(
                                     "where: TodoWhereUniqueInput!",
                                     "update: TodoUpdateDataInput!",
                                     "create: TodoCreateInput!"
                                   ))
  }

  "the update Mutation for a one to one relation with an optional backrelation" should "be generated correctly" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val list = schema.model("List").field_!("listUnique", _.String, isUnique = true).field("optList", _.String)
      val todo = schema.model("Todo").field_!("todoUnique", _.String, isUnique = true).field("optString", _.String)
      list.oneToOneRelation("todo", "does not matter", todo, includeFieldB = false)
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

    schema should containInputType(
      "TodoUpdateOneInput",
      fields = Vector(
        "create: TodoCreateInput",
        "connect: TodoWhereUniqueInput",
        "disconnect: Boolean",
        "delete: Boolean",
        "update: TodoUpdateDataInput"
      )
    )
  }

  "a schema with optional back relations" should "not include any type related to magical back relations" in {
    val project = SchemaDsl.fromStringV11() {
      s"""
         |type List {
         |  id: ID! @id
         |  title: String!
         |}
         |type Todo {
         |  id: ID! @id
         |  name: String
         |  list: List @relation(link: INLINE)
         |}
       """.stripMargin
    }

    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

    schema should not(include(Field.magicalBackRelationPrefix))
  }

}
