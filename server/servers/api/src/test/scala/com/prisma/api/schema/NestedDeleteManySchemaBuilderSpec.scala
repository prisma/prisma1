package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapabilities
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class NestedDeleteManySchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {

  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)
  val schemaBuilder                   = testDependencies.apiSchemaBuilder

  "Nested DeleteMany" should "have scalarWhereFilter" in {
    val project = SchemaDsl.fromStringV11() {
      """
        |type Top {
        |   id: ID! @id
        |   name: String @unique
        |   other: [Other]
        |}
        |
        |type Other{
        |   id: ID! @id
        |   top: Top @relation(link: INLINE)
        |   test: String
        |}
      """
    }

    val schemaBuilder = SchemaBuilderImpl(project, capabilities = ConnectorCapabilities(EmbeddedTypesCapability))(testDependencies)
    val schema        = SchemaRenderer.renderSchema(schemaBuilder.build())

    schema should containInputType(
      "TopUpdateInput",
      fields = Vector(
        "name: String",
        "other: OtherUpdateManyWithoutTopInput",
      )
    )
    schema should containInputType(
      "OtherUpdateManyWithoutTopInput",
      fields = Vector(
        "create: [OtherCreateWithoutTopInput!]",
        "update: [OtherUpdateWithWhereUniqueWithoutTopInput!]",
        "delete: [OtherWhereUniqueInput!]",
        "upsert: [OtherUpsertWithWhereUniqueWithoutTopInput!]",
        "updateMany: [OtherUpdateManyWithWhereNestedInput!]",
        "deleteMany: [OtherScalarWhereInput!]",
        "connect: [OtherWhereUniqueInput!]",
        "disconnect: [OtherWhereUniqueInput!]",
        "set: [OtherWhereUniqueInput!]",
      )
    )
  }

}
