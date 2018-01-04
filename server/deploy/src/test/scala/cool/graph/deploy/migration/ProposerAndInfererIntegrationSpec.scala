package cool.graph.deploy.migration

import cool.graph.shared.models.{MigrationStep, Project}
import org.scalatest.{FlatSpec, Matchers}
import sangria.parser.QueryParser

class ProposerAndInfererIntegrationSpec extends FlatSpec with Matchers {

  "they" should "should propose no UpdateRelation when ambiguous relations are involved" in {
    val schema =
      """
        |type Todo {
        |  comments1: [Comment!]! @relation(name: "TodoToComments1")
        |  comments2: [Comment!]! @relation(name: "TodoToComments2")
        |}
        |type Comment {
        |  text: String
        |  todo1: Todo @relation(name: "TodoToComments1")
        |  todo2: Todo @relation(name: "TodoToComments2")
        |}
      """.stripMargin
    val project = infer(schema)
    val steps   = propose(previous = project, next = schema)

    steps should be(empty)
  }

  def infer(schema: String): Project = {
    val newProject = Project(
      id = "test-project",
      ownerId = "owner"
    )
    infer(newProject, schema)
  }

  def infer(previous: Project, schema: String): Project = {
    val schemaAst = QueryParser.parse(schema).get
    val project   = NextProjectInferer().infer(previous, Renames.empty, schemaAst).getOrElse(sys.error("Infering the project failed."))
    println(project.relations)
    project
  }

  def propose(previous: Project, next: String): Vector[MigrationStep] = {
    val nextProject = infer(previous, next)
    MigrationStepsProposer().propose(
      currentProject = previous,
      nextProject = nextProject,
      renames = Renames.empty
    )
  }
}
