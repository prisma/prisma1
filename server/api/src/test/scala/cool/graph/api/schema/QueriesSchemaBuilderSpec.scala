package cool.graph.api.schema

import cool.graph.api.ApiBaseSpec
import cool.graph.util.GraphQLSchemaAssertions
import org.scalatest.{FlatSpec, Matchers}

class QueriesSchemaBuilderSpec extends FlatSpec with Matchers with ApiBaseSpec with GraphQLSchemaAssertions {
  val schemaBuilder = testDependencies.apiSchemaBuilder
}
