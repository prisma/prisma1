package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability
import com.prisma.shared.models.ConnectorCapability.MongoJoinRelationLinksCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class SubscriptionsSchemaBuilderSpec extends WordSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  override def doNotRunForCapabilities: Set[ConnectorCapability] = Set(MongoJoinRelationLinksCapability)

  val schemaBuilder = testDependencies.apiSchemaBuilder

  "the single item query for a model" must {
    "be generated correctly" in {
      val project = SchemaDsl.fromBuilder { schema =>
        schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))

      println(schema)

      schema should containSubscription("todo(where: TodoSubscriptionWhereInput): TodoSubscriptionPayload")

    }

    "have correct payload" in {
      val project = SchemaDsl.fromBuilder { schema =>
        val testSchema = schema.model("Todo")
      }

      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should containType("TodoSubscriptionPayload",
                                fields = Vector(
                                  "mutation: MutationType!",
                                  "node: Todo",
                                  "updatedFields: [String!]",
                                  "previousValues: TodoPreviousValues"
                                ))

      schema should containType("TodoPreviousValues",
                                fields = Vector(
                                  "id: ID!"
                                ))

      schema should containEnum("MutationType", values = Vector("CREATED", "UPDATED", "DELETED"))

      schema should containInputType(
        "TodoSubscriptionWhereInput",
        fields = Vector(
          "AND: [TodoSubscriptionWhereInput!]",
          "OR: [TodoSubscriptionWhereInput!]",
          "mutation_in: [MutationType!]",
          "updatedFields_contains: String",
          "updatedFields_contains_every: [String!]",
          "updatedFields_contains_some: [String!]",
          "node: TodoWhereInput"
        )
      )

    }
  }
}
