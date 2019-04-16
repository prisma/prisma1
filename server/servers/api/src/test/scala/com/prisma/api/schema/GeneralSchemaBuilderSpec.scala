package com.prisma.api.schema

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import com.prisma.util.GraphQLSchemaMatchers
import org.scalatest.{Matchers, WordSpec}
import sangria.renderer.SchemaRenderer

class GeneralSchemaBuilderSpec extends WordSpec with Matchers with ApiSpecBase with GraphQLSchemaMatchers {
  val schemaBuilder = testDependencies.apiSchemaBuilder

  "The types of a schema" must {

    "must not crash when using type names that can not be pluralized" in {
      // this is a limitation of our pluralization lib: https://github.com/atteo/evo-inflector/blob/64ab921bbdeb797e6aa6469eb7a53320b505221a/src/main/java/org/atteo/evo/inflector/English.java#L133
      val project = SchemaDsl.fromStringV11() {
        """
          |type News {
          |  id: ID! @id
          |}
          |
          |type Homework {
          |  id: ID! @id
          |}
          |
          |type Scissors {
          |  id: ID! @id
          |}
        """.stripMargin
      }
      val schema = SchemaRenderer.renderSchema(schemaBuilder(project))
      schema should include("news(where: ")
      schema should include("newses(where: ")
      schema should include("homework(where: ")
      schema should include("homeworks(where: ")
      schema should include("scissors(where: ")
      schema should include("scissorses(where: ")
    }
  }
}
