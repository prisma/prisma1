package com.prisma.api.mutations

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models.Project
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateManyListSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl.fromString() { """type MyObject {
                                                  |  challengeDescription: String! 
                                                  |  challengeIdentifier: String! 
                                                  |  tags: [Tag!]!
                                                  |  points: Int! 
                                                  |}
                                                  |
                                                  |enum Tag{
                                                  | A
                                                  | B
                                                  |}""" }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override protected def beforeEach(): Unit = {
    super.beforeEach()
    database.truncate(project)
  }

  "The updateMany Mutation" should "also update listvalues" in {

    server.query("""mutation{createMyObject(data:{challengeDescription: "Test", challengeIdentifier: "Test", points: 2}){points}}""", project)

    server.query(
      """|mutation a {
         |         updateManyMyObjects(
         |                data:{
         |                    challengeDescription: "Draw a team crest on your DO GOOD flag ", 
         |                    points: 1,
         |                    tags: { set: [A,B] }
         |                }
         |            )
         |            {
         |                count
         |            }
         |}""",
      project
    )

    server.query("""{myObjects{points, tags}}""", project)
  }
}
