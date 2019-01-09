package com.prisma.api.mutations

import com.prisma.IgnoreMongo
import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdatedAtShouldChangeSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = SchemaDsl.fromString() {

    """type Top {
      |id: ID! @unique
      |top: String! @unique
      |bottom: Bottom
      |createdAt: DateTime!
      |updatedAt: DateTime!
      |}
      |
      |type Bottom {
      |id: ID! @unique
      |bottom: String! @unique
      |top: Top
      |createdAt: DateTime!
      |updatedAt: DateTime!
      |}
      |"""
  }

  database.setup(project)

  "Updating a data item" should "change it's updatedAt value" in {
    val updatedAt = server.query("""mutation a {createTop(data: { top: "top1" }) {updatedAt}}""", project).pathAsString("data.createTop.updatedAt")

    val changedUpdatedAt = server
      .query(
        s"""mutation b {
         |  updateTop(
         |    where: { top: "top1" }
         |    data: { top: "top10" }
         |  ) {
         |    updatedAt
         |  }
         |}
      """,
        project
      )
      .pathAsString("data.updateTop.updatedAt")

    updatedAt should not equal changedUpdatedAt
  }

  "Upserting a data item" should "change it's updatedAt value" in {
    val updatedAt = server.query("""mutation a {createTop(data: { top: "top3" }) {updatedAt}}""", project).pathAsString("data.createTop.updatedAt")

    val changedUpdatedAt = server
      .query(
        s"""mutation b {
           |  upsertTop(
           |    where: { top: "top3" }
           |    update: { top: "top30" }
           |    create: { top: "Should not matter" }
           |  ) {
           |    updatedAt
           |  }
           |}
      """,
        project
      )
      .pathAsString("data.upsertTop.updatedAt")

    updatedAt should not equal changedUpdatedAt
  }

  "UpdateMany" should "change updatedAt values" in {
    val updatedAt = server.query("""mutation a {createTop(data: { top: "top5" }) {updatedAt}}""", project).pathAsString("data.createTop.updatedAt")

    val res = server
      .query(
        s"""mutation b {
           |  updateManyTops(
           |    where: { top: "top5" }
           |    data: { top: "top50" }
           |  ) {
           |    count
           |  }
           |}
      """,
        project
      )

    res.toString should be("""{"data":{"updateManyTops":{"count":1}}}""")

    val changedUpdatedAt = server
      .query(
        s"""query{
           |  top(where: { top: "top50" }) {
           |    updatedAt
           |  }
           |}
      """,
        project
      )
      .pathAsString("data.top.updatedAt")

    updatedAt should not equal changedUpdatedAt
  }
}
