package writes.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class NonEmbeddedUpdatedAtShouldChangeSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val project = SchemaDsl.fromStringV11() {

    """type Top {
      |  id: ID! @id
      |  top: String! @unique
      |  bottom: Bottom @relation(link: INLINE)
      |  createdAt: DateTime! @createdAt
      |  updatedAt: DateTime! @updatedAt
      |}
      |
      |type Bottom {
      |  id: ID! @id
      |  bottom: String! @unique
      |  top: Top
      |  createdAt: DateTime! @createdAt
      |  updatedAt: DateTime! @updatedAt
      |}
      |"""
  }

  database.setup(project)

  "Updating a nested data item" should "change it's updatedAt value" in {
    val updatedAt = server
      .query("""mutation a {createTop(data: { top: "top2", bottom: {create:{bottom: "Bottom2"}} }) {bottom{updatedAt}}}""", project)
      .pathAsString("data.createTop.bottom.updatedAt")

    val changedUpdatedAt = server
      .query(
        s"""mutation b {
           |  updateTop(
           |    where: { top: "top2" }
           |    data: { bottom: {update:{bottom: "bottom20"}} }
           |  ) {
           |    bottom{
           |      updatedAt
           |    }
           |  }
           |}
      """,
        project
      )
      .pathAsString("data.updateTop.bottom.updatedAt")

    updatedAt should not equal changedUpdatedAt
  }

  "Upserting a nested data item" should "change it's updatedAt value" in {
    val updatedAt = server
      .query("""mutation a {createTop(data: { top: "top4", bottom: {create:{bottom: "Bottom4"}} }) {bottom{updatedAt}}}""", project)
      .pathAsString("data.createTop.bottom.updatedAt")

    val changedUpdatedAt = server
      .query(
        s"""mutation b {
           |  updateTop(
           |    where: { top: "top4" }
           |    data: { bottom: {upsert:{create:{bottom: "Should not matter"}, update:{bottom: "Bottom40"}}} }
           |  ) {
           |    bottom{
           |      updatedAt
           |    }
           |  }
           |}
      """,
        project
      )
      .pathAsString("data.updateTop.bottom.updatedAt")

    updatedAt should not equal changedUpdatedAt
  }
}
