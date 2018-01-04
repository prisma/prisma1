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

  "they" should "handle ambiguous relations correctly" in {
    val previousSchema =
      """
        |type Todo {
        |  title: String
        |}
        |type Comment {
        |  text: String
        |}
      """.stripMargin
    val project = infer(previousSchema)

    val nextSchema =
      """
        |type Todo {
        |  title: String
        |  comment1: Comment @relation(name: "TodoToComment1")
        |  comment2: Comment @relation(name: "TodoToComment2")
        |}
        |type Comment {
        |  text: String
        |  todo1: Todo @relation(name: "TodoToComment1")
        |  todo2: Todo @relation(name: "TodoToComment2")
        |}
      """.stripMargin
    val steps = propose(previous = project, next = nextSchema)
    steps should have(size(6))
    steps should contain allOf (
      CreateField(
        model = "Todo",
        name = "comment1",
        typeName = "Relation",
        isRequired = false,
        isList = false,
        isUnique = false,
        relation = Some("TodoToComment1"),
        defaultValue = None,
        enum = None
      ),
      CreateField(
        model = "Todo",
        name = "comment2",
        typeName = "Relation",
        isRequired = false,
        isList = false,
        isUnique = false,
        relation = Some("TodoToComment2"),
        defaultValue = None,
        enum = None
      ),
      CreateField(
        model = "Comment",
        name = "todo1",
        typeName = "Relation",
        isRequired = false,
        isList = false,
        isUnique = false,
        relation = Some("TodoToComment1"),
        defaultValue = None,
        enum = None
      ),
      CreateField(
        model = "Comment",
        name = "todo2",
        typeName = "Relation",
        isRequired = false,
        isList = false,
        isUnique = false,
        relation = Some("TodoToComment2"),
        defaultValue = None,
        enum = None
      ),
      CreateRelation(
        name = "TodoToComment1",
        leftModelName = "Comment",
        rightModelName = "Todo"
      ),
      CreateRelation(
        name = "TodoToComment2",
        leftModelName = "Comment",
        rightModelName = "Todo"
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
    println(s"fields of next project:")
    nextProject.allFields.foreach(println)
    MigrationStepsProposer().propose(
      currentProject = previous,
      nextProject = nextProject,
      renames = Renames.empty
    )
  }
}
