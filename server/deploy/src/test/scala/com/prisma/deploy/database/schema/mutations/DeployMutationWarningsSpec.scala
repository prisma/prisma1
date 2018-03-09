package com.prisma.deploy.database.schema.mutations

import com.prisma.deploy.specutils.DeploySpecBase
import com.prisma.shared.models.ProjectId
import org.scalatest.{FlatSpec, Matchers}

class DeployMutationWarningsSpec extends FlatSpec with Matchers with DeploySpecBase {

  "DeployMutation" should "hide reserved fields instead of deleting them and reveal them instead of creating them" in {
    val schema = """
                   |type ModelA {
                   |  id: ID! @unique
                   |  b: ModelB
                   |}
                   |
                   |type ModelB {
                   |  id: ID! @unique
                   |  a: ModelA
                   |}
                 """.stripMargin

    val (project, _) = setupProject(schema)
    val nameAndStage = ProjectId.fromEncodedString(project.id)

    val newSchema = """
                      |type ModelA {
                      |  id: ID! @unique
                      |}
                      |
                      |type ModelB {
                      |  id: ID! @unique
                      |}
                    """.stripMargin

    val result1 = server.query(s"""
                                  |mutation {
                                  |  deploy(input:{name: "${nameAndStage.name}", stage: "${nameAndStage.stage}", types: ${formatSchema(newSchema)}}){
                                  |    migration {
                                  |      applied
                                  |    }
                                  |    errors {
                                  |      description
                                  |    }
                                  |  }
                                  |}""".stripMargin)

  }
}
