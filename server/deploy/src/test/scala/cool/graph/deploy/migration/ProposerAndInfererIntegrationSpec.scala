package cool.graph.deploy.migration

import cool.graph.shared.models._
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

  "they" should "only propose an UpdateRelation step when relation directives get removed" in {
    val previousSchema =
      """
        |type Todo {
        |  comments: [Comment!]! @relation(name: "ManualRelationName")
        |}
        |type Comment {
        |  text: String
        |  todo: Todo @relation(name: "ManualRelationName")
        |}
      """.stripMargin
    val project = infer(previousSchema)

    val nextSchema =
      """
        |type Todo {
        |  comments: [Comment!]!
        |}
        |type Comment {
        |  text: String
        |  todo: Todo
        |}
      """.stripMargin
    val steps = propose(previous = project, next = nextSchema)

    steps should have(size(3))
    steps should contain allOf (
      UpdateField(
        model = "Todo",
        name = "comments",
        newName = None,
        typeName = None,
        isRequired = None,
        isList = None,
        isHidden = None,
        isUnique = None,
        relation = Some(Some("_CommentToTodo")),
        defaultValue = None,
        enum = None
      ),
      UpdateField(
        model = "Comment",
        name = "todo",
        newName = None,
        typeName = None,
        isRequired = None,
        isList = None,
        isHidden = None,
        isUnique = None,
        relation = Some(Some("_CommentToTodo")),
        defaultValue = None,
        enum = None
      ),
      UpdateRelation(
        name = "ManualRelationName",
        newName = Some("CommentToTodo"),
        modelAId = None,
        modelBId = None
      )
    )

  }

  "they" should "not propose a DeleteRelation step when relation directives gets added" in {
    val previousSchema =
      """
        |type Todo {
        |  comments: [Comment!]!
        |}
        |type Comment {
        |  text: String
        |  todo: Todo
        |}
      """.stripMargin
    val project = infer(previousSchema)

    val nextSchema =
      """
        |type Todo {
        |  comments: [Comment!]! @relation(name: "ManualRelationName")
        |}
        |type Comment {
        |  text: String
        |  todo: Todo @relation(name: "ManualRelationName")
        |}
      """.stripMargin
    val steps = propose(previous = project, next = nextSchema)

    steps should have(size(3))
    steps should contain allOf (
      UpdateField(
        model = "Todo",
        name = "comments",
        newName = None,
        typeName = None,
        isRequired = None,
        isList = None,
        isHidden = None,
        isUnique = None,
        relation = Some(Some("_ManualRelationName")),
        defaultValue = None,
        enum = None
      ),
      UpdateField(
        model = "Comment",
        name = "todo",
        newName = None,
        typeName = None,
        isRequired = None,
        isList = None,
        isHidden = None,
        isUnique = None,
        relation = Some(Some("_ManualRelationName")),
        defaultValue = None,
        enum = None
      ),
      UpdateRelation(
        name = "CommentToTodo",
        newName = Some("ManualRelationName"),
        modelAId = None,
        modelBId = None
      )
    )
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

    println(s"Relations of infered project:\n  " + project.relations)
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
