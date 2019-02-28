package com.prisma.api.mutations.nonEmbedded

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class VeryManyMutationsSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def doNotRun               = true // we don't run this suite as it takes ages. We rather run it manually.
  override def runOnlyForCapabilities = Set(JoinRelationLinksCapability)

  //Postgres has a limit of 32678 parameters to a query

  "The delete many Mutation" should "delete the items matching the where clause" in {
    val project: Project = SchemaDsl.fromString() {
      """
      |type Top {
      |   id: ID! @unique
      |   int: Int!
      |   middles:[Middle]
      |}
      |
      |type Middle {
      |   id: ID! @unique
      |   int: Int!
      |}
    """
    }
    database.setup(project)

    def createTop(int: Int): Unit = {
      val query =
        s"""mutation a {createTop(data: {
           |  int: $int
           |  middles: {create: [
           |  {int: ${int}1},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: ${int}20},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: $int},
           |  {int: ${int}40}
           |  ]}
           |}) {int}}"""

      server.query(query, project)
    }

    for (int <- 1 to 1000) {
      createTop(int)
    }

    val update = server.query("""mutation {updateManyMiddles(where: { int_gt: 100 } data:{int: 500}){count}}""", project)
    update.pathAsLong("data.updateManyMiddles.count") should equal(36291)

    val result = server.query("""mutation {deleteManyMiddles(where: { int_gt: 100 }){count}}""", project)
    result.pathAsLong("data.deleteManyMiddles.count") should equal(36291)
  }

  "A cascading delete" should "not hit the parameter limit" in {

    val project: Project = SchemaDsl.fromString() {
      """
        |type Top {
        |   id: ID! @unique
        |   int: Int @unique
        |   middles:[Middle]   @relation(name: "TopToMiddle", onDelete: CASCADE)
        |}
        |
        |type Middle {
        |   id: ID! @unique
        |   int: Int! @unique
        |   top: Top @relation(name: "TopToMiddle")
        |   bottom: [Bottom] @relation(name: "MiddleToBottom", onDelete: CASCADE)
        |}
        |
        |type Bottom {
        |   id: ID! @unique
        |   middle: Middle @relation(name: "MiddleToBottom")
        |   int: Int!
        |}
      """
    }
    database.setup(project)

    val top = server.query("""mutation {createTop(data:{int: 1}){int}}""", project)

    def createMiddle(int: Int) = server.query(s"""mutation {createMiddle(data:{int: $int top: {connect:{int: 1}}}){int}}""", project)

    for (int <- 1 to 200) {
      createMiddle(int)
    }

    def createBottom(int: Int) = {

      server.query(
        s"""mutation{
           |a: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |b: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |c: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |d: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |e: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |f: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |g: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |h: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |i: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |j: createBottom(data:{int: $int$int middle: {connect:{int: $int}}}){int}
           |}
         """.stripMargin,
        project
      )
    }

    for (int <- 1 to 200) {
      for (int <- 1 to 20) {
        createBottom(int)
      }
    }

    server.query("""mutation {deleteTop(where:{int: 1}){int}}""", project)

    server.query("""query {tops{int}}""", project).toString should be("""{"data":{"tops":[]}}""")

    server.query("""query {middles{int}}""", project).toString should be("""{"data":{"middles":[]}}""")

    server.query("""query {bottoms{int}}""", project).toString should be("""{"data":{"bottoms":[]}}""")

  }

  "A cascading delete" should "not hit the parameter limit 2" in {

    val project: Project = SchemaDsl.fromString() {
      """
        |type Top{
        |   id: ID! @unique
        |   int: Int @unique
        |   as: [A] @relation(name: "Top" onDelete: CASCADE)
        |}
        |
        |type A {
        |   id: ID! @unique
        |   int: Int @unique
        |   bs:[B]  @relation(name: "A" onDelete: CASCADE)
        |}
        |
        |type B {
        |   id: ID! @unique
        |   int: Int
        |   cs: [C] @relation(name: "B" onDelete: CASCADE)
        |}
        |
        |type C {
        |   id: ID! @unique
        |   int: Int
        |   ds: [D] @relation(name: "C" onDelete: CASCADE)
        |}
        |
        |type D {
        |   id: ID! @unique
        |   int: Int
        |}
      """
    }
    database.setup(project)

    def createAs(int: Int) = {

      val d = s"{int:1}"
      val c = s"{int: 1, ds: {create:[$d,$d,$d,$d,$d,$d,$d,$d,$d,$d]}}"
      val b = s"{int: 1, cs: {create:[$c,$c,$c,$c,$c,$c,$c,$c,$c,$c]}}"

      val as = s"""
                  |mutation {createA(data:{
                  |   int: $int
                  |   bs: {create:[$b,$b,$b,$b,$b,$b,$b,$b,$b,$b]}
                  |}){int}}"""

      server.query(as.stripMargin, project)
    }

    for (int <- 1 to 40) {
      createAs(int)
    }

    def as(string: String, int: Int = 40): String = int match {
      case 1 => string ++ s"{int: $int}"
      case _ => as(string ++ s"{int: $int},", int - 1)
    }

    val topQuery = s"""
                      |mutation {createTop(data:{
                      |   int: 1
                      |   as: {connect:[${as("")}]}
                      |}){int}}"""

    server.query(topQuery.stripMargin, project)

    server.query("""mutation {deleteTop(where:{int: 1}){int}}""", project)

    server.query("""query {tops{int}}""", project).toString should be("""{"data":{"tops":[]}}""")
    server.query("""query {as{int}}""", project).toString should be("""{"data":{"as":[]}}""")
    server.query("""query {bs{int}}""", project).toString should be("""{"data":{"bs":[]}}""")
    server.query("""query {cs{int}}""", project).toString should be("""{"data":{"cs":[]}}""")
    server.query("""query {ds{int}}""", project).toString should be("""{"data":{"ds":[]}}""")
  }
}
