package com.prisma.api.mutations.embedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class MongoBug1 extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(EmbeddedTypesCapability)

  "Mixing Join and Embedded" should "work 1" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |  top: String @unique
        |  otherTops: [OtherTop] @mongoRelation(field: "otherTops")
        |}
        |
        |
        |type OtherTop {
        |  id: ID! @unique
        |  otherTop: String @unique
        |  nested: Nested
        |  }
        |
        |
        |type Nested @embedded {
        |  id: ID! @unique
        |  nested: String @unique
        |}
        |"""
    }

    database.setup(project)

    val embeddedInArrayOfJoinRelations = server.query(
      """mutation {
        |  createTop(
        |    data: {
        |      top: "Top3"
        |      otherTops: {
        |        create: [
        |          {
        |            otherTop: "OtherTop3"
        |            nested: {
        |              create: {
        |                nested:"Nested2"
        |              }
        |            }
        |          }
        |        ]
        |      }
        |    }
        |  ) {
        |    top
        |    otherTops{
        |     otherTop
        |     nested{
        |       nested
        |     }
        |    }
        |  }
        |}""",
      project
    )

    embeddedInArrayOfJoinRelations.toString should be(
      """{"data":{"createTop":{"top":"Top3","otherTops":[{"otherTop":"OtherTop3","nested":{"nested":"Nested2"}}]}}}""")
  }

  "Mixing Join and Embedded" should "work 2" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |  top: String @unique
        |  otherTops: [OtherTop] @mongoRelation(field: "otherTops")
        |}
        |
        |
        |type OtherTop {
        |  id: ID! @unique
        |  otherTop: String @unique
        |  nested: Nested
        |  }
        |
        |
        |type Nested @embedded {
        |  id: ID! @unique
        |  nested: String @unique
        |}
        |"""
    }

    database.setup(project)

    val justInbox = server.query(
      """mutation otherTop {
            |  first: createOtherTop(
            |    data: {
            |      otherTop: "OtherTop"
            |      nested: {
            |        create: {
            |          nested: "Nested"
            |        }
            |      }}
            |  ) {
            |    otherTop
            |    nested{
            |       nested
            |    }
            |  }
            |}""",
      project
    )

    justInbox.toString should be("""{"data":{"first":{"otherTop":"OtherTop","nested":{"nested":"Nested"}}}}""")
  }

  "Mixing Join and Embedded" should "work 3" in {

    val project = SchemaDsl.fromString() {
      """type Top {
        |  top: String @unique
        |  otherTops: [OtherTop] @mongoRelation(field: "otherTops")
        |}
        |
        |
        |type OtherTop {
        |  id: ID! @unique
        |  otherTop: String @unique
        |  nested: Nested
        |  }
        |
        |
        |type Nested @embedded {
        |  id: ID! @unique
        |  nested: String @unique
        |}
        |"""
    }

    database.setup(project)

    val inboxInArray = server.query(
      """mutation {
            |  createTop(
            |    data: {
            |      top: "Top"
            |      otherTops: {
            |        create: [
            |          {
            |            otherTop: "OtherTop2"
            |          }
            |        ]
            |      }
            |    }
            |  ) {
            |    top
            |    otherTops{
            |       otherTop
            |    }
            |  }
            |}""",
      project
    )

    inboxInArray.toString should be("""{"data":{"createTop":{"top":"Top","otherTops":[{"otherTop":"OtherTop2"}]}}}""")
  }

  "Mixing Join and Embedded" should "work 4" in {

    val project = SchemaDsl.fromString() {
      """type Item {
        |  subItems: [SubItem]
        |}
        |
        |type SubItem @embedded {
        |  subSubItem: SubSubItem 
        |}
        |type SubSubItem @embedded {
        |  text: String
        |}"""
    }

    database.setup(project)

    val test = server.query(
      """mutation {
        |  createItem(
        |    data: {
        |      subItems: { create: [{ subSubItem: { create: { text: "test" } } }] }
        |    }
        |  ) {
        |    subItems {
        |      subSubItem {
        |        text
        |      }
        |    }
        |  }
        |}""",
      project
    )

    test.toString should be("""{"data":{"createItem":{"subItems":[{"subSubItem":{"text":"test"}}]}}}""")
  }

  "Mixing Join and Embedded" should "work 5" in {

    val project = SchemaDsl.fromString() {
      """type Item {
        |  subItem: SubItem
        |}
        |
        |type SubItem @embedded {
        |  subSubItem: SubSubItem
        |}
        |type SubSubItem @embedded {
        |  text: String
        |}"""
    }

    database.setup(project)

    val test = server.query(
      """mutation {
        |  createItem(
        |    data: {
        |      subItem: { create: { subSubItem: { create: { text: "test" } } } }
        |    }
        |  ) {
        |    subItem {
        |      subSubItem {
        |        text
        |      }
        |    }
        |  }
        |}""",
      project
    )

    test.toString should be("""{"data":{"createItem":{"subItem":{"subSubItem":{"text":"test"}}}}}""")
  }

  "Mixing Join and Embedded" should "work 6" in {

    val project = SchemaDsl.fromString() {
      """type Item {
        |  subItems: [SubItem] @mongoRelation(field: "subItems")
        |}
        |
        |type SubItem {
        |  subSubItem: SubSubItem
        |}
        |type SubSubItem @embedded {
        |  text: String
        |}"""
    }

    database.setup(project)

    val test = server.query(
      """mutation {
        |  createItem(
        |    data: {
        |      subItems: { create: [{ subSubItem: { create: { text: "test" } } }] }
        |    }
        |  ) {
        |    subItems {
        |      subSubItem {
        |        text
        |      }
        |    }
        |  }
        |}""",
      project
    )

    test.toString should be("""{"data":{"createItem":{"subItems":[{"subSubItem":{"text":"test"}}]}}}""")
  }

}
