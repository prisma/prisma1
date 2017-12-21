package cool.graph.deploy.migration

import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalactic.Or
import org.scalatest.{FlatSpec, Matchers, WordSpec}
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
      val result = infer(emptyProject, types)
      result.get.getRelationByName("MyNameForTodoToComments").isDefined should be(true)
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
      val field1   = project.getModelByName_!("Todo").getFieldByName_!("comments")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = project.getModelByName_!("Comment").getFieldByName_!("todo")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }
  }

  "if a given relation does already exist, the inferer" should {
    val project = SchemaDsl() { schema =>
      val comment = schema.model("comment")
      schema.model("Todo").oneToManyRelation("comments", "todo", comment, relationName = Some("CommentToTodo"))
    }

    "infer the existing name of the relation although the type names changed" in {
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

      val newProject = infer(project, types).get
      newProject.relations.foreach(println(_))

      val relation = newProject.getRelationByName_!("CommentToTodo")
      val field1   = newProject.getModelByName_!("TodoNew").getFieldByName_!("comments")
      field1.isList should be(true)
      field1.relation should be(Some(relation))

      val field2 = newProject.getModelByName_!("CommentNew").getFieldByName_!("todo")
      field2.isList should be(false)
      field2.relation should be(Some(relation))
    }
  }

  def infer(project: Project, types: String): Or[Project, ProjectSyntaxError] = {
    val document = QueryParser.parse(types).get
    inferer.infer(project, document)
  }
}
