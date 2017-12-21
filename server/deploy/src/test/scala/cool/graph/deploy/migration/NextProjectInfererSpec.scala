package cool.graph.deploy.migration

import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalactic.Or
import org.scalatest.{Matchers, WordSpec}
import sangria.parser.QueryParser

class NextProjectInfererSpec extends WordSpec with Matchers {

  val inferer      = NextProjectInferer()
  val emptyProject = SchemaDsl().buildProject()

  "if a given relation does not exist yet, the inferer" should {
    "infer relations with the given name if a relation directive is provided" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!] @relation(name:"MyNameForTodoToComments")
          |}
          |
          |type Comment {
          |  todo: Todo! @relation(name:"MyNameForTodoToComments")
          |}
        """.stripMargin.trim()
      val project = infer(emptyProject, types).get
      project.relations.foreach(println(_))

      val relation = project.getRelationByName_!("MyNameForTodoToComments")
      relation.modelAId should equal("Comment")
      relation.modelBId should equal("Todo")
    }

    "infer relations with an auto generated name if no relation directive is given" in {
      val types =
        """
          |type Todo {
          |  comments: [Comment!]
          |}
          |
          |type Comment {
          |  todo: Todo!
          |}
        """.stripMargin.trim()
      val project = infer(emptyProject, types).get
      project.relations.foreach(println(_))

      val relation = project.getRelationByName_!("CommentToTodo")
      relation.modelAId should equal("Comment")
      relation.modelBId should equal("Todo")

      val field1 = project.getModelByName_!("Todo").getFieldByName_!("comments")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = project.getModelByName_!("Comment").getFieldByName_!("todo")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }
  }

  "if a given relation does already exist, the inferer" should {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("Comment")
      schema.model("Todo").oneToManyRelation("comments", "todo", comment, relationName = Some("CommentToTodo"))
    }

    "infer the existing relation and update it accordingly when the type names change" in {
      val types =
        """
          |type TodoNew {
          |  comments: [CommentNew!]
          |}
          |
          |type CommentNew {
          |  todo: TodoNew!
          |}
        """.stripMargin

      val renames = Renames(
        models = Vector(
          Rename(previous = "Todo", next = "TodoNew"),
          Rename(previous = "Comment", next = "CommentNew")
        )
      )
      val newProject = infer(project, types, renames).get
      newProject.relations.foreach(println(_))

      val relation = newProject.getRelationByName_!("CommentNewToTodoNew")
      relation.modelAId should be("TodoNew")
      relation.modelBId should be("CommentNew")

      val field1 = newProject.getModelByName_!("TodoNew").getFieldByName_!("comments")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = newProject.getModelByName_!("CommentNew").getFieldByName_!("todo")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }

    "infer the existing relation although the type and field names changed" in {
      val types =
        """
          |type TodoNew {
          |  commentsNew: [CommentNew!]
          |}
          |
          |type CommentNew {
          |  todoNew: TodoNew!
          |}
        """.stripMargin

      val renames = Renames(
        models = Vector(
          Rename(previous = "Todo", next = "TodoNew"),
          Rename(previous = "Comment", next = "CommentNew")
        ),
        fields = Vector(
          FieldRename(previousModel = "Todo", previousField = "comments", nextModel = "TodoNew", nextField = "commentsNew"),
          FieldRename(previousModel = "Comment", previousField = "todo", nextModel = "CommentNew", nextField = "todoNew")
        )
      )
      val newProject = infer(project, types, renames).get
      newProject.relations.foreach(println(_))

      val relation = newProject.getRelationByName_!("CommentNewToTodoNew")
      relation.modelAId should be("TodoNew")
      relation.modelBId should be("CommentNew")

      val field1 = newProject.getModelByName_!("TodoNew").getFieldByName_!("commentsNew")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = newProject.getModelByName_!("CommentNew").getFieldByName_!("todoNew")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }
  }

  def infer(project: Project, types: String, renames: Renames = Renames.empty): Or[Project, ProjectSyntaxError] = {
    val document = QueryParser.parse(types).get
    inferer.infer(project, renames, document)
  }
}
