package writes.nonEmbedded

import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.JoinRelationLinksCapability
import util._

class UpdateManyRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  val schema =
    """type Top{
      |   id: ID! @id
      |   top: String!
      |   bottom: Bottom @relation(link: INLINE)
      |}
      |
      |type Bottom{
      |   id: ID! @id
      |   bottom: String!
      |   top: Top
      |   veryBottom: VeryBottom @relation(link: INLINE)
      |}
      |
      |type VeryBottom{
      |   id: ID! @id
      |   veryBottom: String!
      |   bottom: Bottom
      |}""".stripMargin

  lazy val project: Project = SchemaDsl.fromStringV11() { schema }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "The updateMany Mutation" should "update the items matching the where relation filter" in {
    createTop("top1")
    createTop("top2")

    server.query(
      s"""mutation {
         |  createTop(
         |    data: {
         |      top: "top3"
         |      bottom: {
         |        create: {bottom: "bottom1"}
         |      }
         |    }
         |  ) {
         |    id
         |  }
         |}
      """.stripMargin,
      project
    )

    val filter = """{ bottom: null }"""

    val firstCount       = topUpdatedCount
    val filterQueryCount = server.query(s"""{tops(where: $filter){id}}""", project).pathAsSeq("data.tops").length
    val filterUpdatedCount = server
      .query(s"""mutation {updateManyTops(where: $filter, data: { top: "updated" }){count}}""".stripMargin, project)
      .pathAsLong("data.updateManyTops.count")
    val lastCount = topUpdatedCount

    firstCount should be(0)
    filterQueryCount should be(2)
    firstCount + filterQueryCount should be(lastCount)
    lastCount - firstCount should be(filterUpdatedCount)
  }

  "The updateMany Mutation" should "update all items if the filter is empty" in {
    createTop("top1")
    createTop("top2")

    server.query(
      s"""mutation {
         |  createTop(
         |    data: {
         |      top: "top3"
         |      bottom: {
         |        create: {bottom: "bottom1"}
         |      }
         |    }
         |  ) {
         |    id
         |  }
         |}
      """.stripMargin,
      project
    )

    val firstCount       = topUpdatedCount
    val filterQueryCount = server.query(s"""{tops{id}}""", project).pathAsSeq("data.tops").length
    val filterUpdatedCount = server
      .query(s"""mutation {updateManyTops(data: { top: "updated" }){count}}""".stripMargin, project)
      .pathAsLong("data.updateManyTops.count")
    val lastCount = topUpdatedCount

    firstCount should be(0)
    filterQueryCount should be(3)
    firstCount + filterQueryCount should be(lastCount)
    lastCount - firstCount should be(filterUpdatedCount)
  }

  "The updateMany Mutation" should "work for deeply nested filters" in {
    createTop("top1")
    createTop("top2")

    server.query(
      s"""mutation {
         |  createTop(
         |    data: {
         |      top: "top3"
         |      bottom: {
         |        create: {
         |        bottom: "bottom1"
         |        veryBottom: {create: {veryBottom: "veryBottom"}}}
         |      }
         |    }
         |  ) {
         |    id
         |  }
         |}
      """.stripMargin,
      project
    )

    val filter = """{ bottom: {veryBottom: {veryBottom: "veryBottom"}}}"""

    val firstCount       = topUpdatedCount
    val filterQueryCount = server.query(s"""{tops(where: $filter){id}}""", project).pathAsSeq("data.tops").length
    val filterUpdatedCount = server
      .query(s"""mutation {updateManyTops(where: $filter, data: { top: "updated" }){count}}""".stripMargin, project)
      .pathAsLong("data.updateManyTops.count")
    val lastCount = topUpdatedCount

    firstCount should be(0)
    filterQueryCount should be(1)
    firstCount + filterQueryCount should be(lastCount)
    lastCount - firstCount should be(filterUpdatedCount)
  }

  def topCount: Int        = server.query("{ tops { id } }", project).pathAsSeq("data.tops").size
  def topUpdatedCount: Int = server.query("""{ tops(where: {top: "updated"}) { id } }""", project).pathAsSeq("data.tops").size

  def createTop(top: String): Unit = {
    server.query(
      s"""mutation {
        |  createTop(
        |    data: {
        |      top: "$top"
        |    }
        |  ) {
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )
  }
}
