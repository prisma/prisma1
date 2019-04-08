package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{FlatSpec, Matchers}
import sangria.renderer.SchemaRenderer

class ObjectTypesSchemaBuilderSpec extends FlatSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  val project = SchemaDsl.fromStringV11() {
    """
      |type Todo {
      |  id: ID! @id
      |  title: String!
      |  user: User! @relation(name: "TodoToUser1" link: INLINE)
      |  users: [User] @relation(name: "TodoToUser2")
      |}
      |
      |type User {
      |  id: ID! @id
      |  name: String!
      |  todo: Todo @relation(name: "TodoToUser2" link: INLINE)
      |}
    """.stripMargin
  }

  "an object type" should "expose fields for one relation fields" in {
    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
    schema should containType("Todo", interface = "Node", fields = Vector("user: User!"))
  }

  "an object type" should "expose fields for many relation fields with where and pagination arguments" in {
    val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
    schema should containType(
      "Todo",
      interface = "Node",
      fields = Vector("users(where: UserWhereInput, orderBy: UserOrderByInput, skip: Int, after: String, before: String, first: Int, last: Int): [User!]")
    )
  }
}
