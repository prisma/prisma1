package cool.graph.deploy.database.schema.queries

import cool.graph.deploy.specutils.DeploySpecBase
import cool.graph.shared.models.{Migration, ProjectId}
import org.scalatest.{FlatSpec, Matchers}

class ListProjectsSpec extends FlatSpec with Matchers with DeploySpecBase {
  "ListProjects" should "an empty list with no projects" in {
    val result = server.query(s"""
       |query {
       |  listProjects {
       |    name
       |    stage
       |  }
       |}
      """.stripMargin)

    result.pathAsSeq("data.listProjects") should have(size(0))
  }

  "MigrationStatus" should "return all projects" in {
    val project  = setupProject(basicTypesGql)
    val project2 = setupProject(basicTypesGql)
    val project3 = setupProject(basicTypesGql)
    val result   = server.query(s"""
       |query {
       |  listProjects {
       |    name
       |    stage
       |  }
       |}
      """.stripMargin)

    result.pathAsSeq("data.listProjects").map(p => s"${p.pathAsString("name")}@${p.pathAsString("stage")}") should contain allOf (
      project.id,
      project2.id,
      project3.id
    )
  }
}
