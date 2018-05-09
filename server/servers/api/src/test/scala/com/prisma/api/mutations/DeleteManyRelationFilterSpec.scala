package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteManyRelationFilterSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project: Project = SchemaDsl.fromBuilder { schema =>
    val top        = schema.model("Top").field_!("top", _.String, isUnique = true)
    val bottom     = schema.model("Bottom").field_!("bottom", _.String, isUnique = true)
    val veryBottom = schema.model("VeryBottom").field_!("veryBottom", _.String, isUnique = true)
    top.oneToOneRelation("bottom", "top", bottom)
    bottom.oneToOneRelation("veryBottom", "bottom", veryBottom)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "The delete many Mutation" should "delete the items matching the where relation filter" in {
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

    val firstCount        = topCount
    val filterQueryCount  = server.query(s"""{tops(where: $filter){id}}""", project).pathAsSeq("data.tops").length
    val filterDeleteCount = server.query(s"""mutation {deleteManyTops(where: $filter){count}}""".stripMargin, project).pathAsLong("data.deleteManyTops.count")
    val lastCount         = topCount

    firstCount should be(3)
    firstCount - filterQueryCount should be(lastCount)
    firstCount - filterDeleteCount should be(lastCount)
  }

  "The delete many Mutation" should "delete all items if the filter is empty" in {
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

    val firstCount        = topCount
    val filterQueryCount  = server.query(s"""{tops{id}}""", project).pathAsSeq("data.tops").length
    val filterDeleteCount = server.query(s"""mutation {deleteManyTops{count}}""".stripMargin, project).pathAsLong("data.deleteManyTops.count")
    val lastCount         = topCount

    firstCount should be(3)
    firstCount - filterQueryCount should be(lastCount)
    firstCount - filterDeleteCount should be(lastCount)
  }

  "The delete many Mutation" should "work for deeply nested filters" in {
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

    val firstCount        = topCount
    val filterQueryCount  = server.query(s"""{tops(where: $filter){id}}""", project).pathAsSeq("data.tops").length
    val filterDeleteCount = server.query(s"""mutation {deleteManyTops(where: $filter){count}}""".stripMargin, project).pathAsLong("data.deleteManyTops.count")
    val lastCount         = topCount

    firstCount should be(3)
    firstCount - filterQueryCount should be(lastCount)
    firstCount - filterDeleteCount should be(lastCount)
  }

  "The delete many Mutation" should "work for named filters" in {
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

    val filter = """{ bottom: {veryBottom: {veryBottom_not: null}}}"""

    val firstCount        = topCount
    val filterQueryCount  = server.query(s"""{tops(where: $filter){id}}""", project).pathAsSeq("data.tops").length
    val filterDeleteCount = server.query(s"""mutation {deleteManyTops(where: $filter){count}}""".stripMargin, project).pathAsLong("data.deleteManyTops.count")
    val lastCount         = topCount

    firstCount should be(3)
    firstCount - filterQueryCount should be(lastCount)
    firstCount - filterDeleteCount should be(lastCount)
  }

  def topCount: Int = server.query("{ tops { id } }", project).pathAsSeq("data.tops").size

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
