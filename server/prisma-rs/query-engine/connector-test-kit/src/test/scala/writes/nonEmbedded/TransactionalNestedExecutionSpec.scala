package writes.nonEmbedded

import org.joda.time.{DateTime, DateTimeZone}
import org.scalatest.{FlatSpec, Matchers}
import util.ConnectorCapability.TransactionalExecutionCapability
import util._

class TransactionalNestedExecutionSpec extends FlatSpec with Matchers with ApiSpecBase {

  override def runOnlyForCapabilities = Set(TransactionalExecutionCapability)

  //At the moment we are only inserting the inner where, the outer condition is checked separately
  //the up front check for the outer where is still needed to provide return values

  // - put a catch all handling on it in the end?
  //Test the parsing of the exception for different datatypes -> DateTime, Json problematic
  def projectFn(tpe: String) = SchemaDsl.fromStringV11() {
    s"""
        |model Todo {
        |  id          String @id @default(cuid())
        |  innerString String
        |  innerUnique $tpe?  @unique
        |  notes       Note[]
        |}
        |
        |model Note {
        |  id          String @id @default(cuid())
        |  outerString String
        |  outerUnique $tpe?  @unique
        |  todos       Todo[]
        |}
        |
        |enum SomeEnum {
        |  A
        |  B
        |  C
        |}
      """.stripMargin
  }

  "a many to many relation" should "fail gracefully on wrong STRING where and assign error correctly and not execute partially" in {
    val outerWhere        = """"Outer Unique""""
    val innerWhere        = """"Inner Unique""""
    val falseWhere        = """"False  Where""""
    val falseWhereInError = """False  Where"""

    val project = projectFn("String")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong INT where and assign error correctly and not execute partially" in {
    val outerWhere        = 1
    val innerWhere        = 2
    val falseWhere        = 3
    val falseWhereInError = 3

    val project = projectFn("Int")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong FLOAT where and assign error correctly and not execute partially" in {
    val outerWhere        = 1.0
    val innerWhere        = 2.0
    val falseWhere        = 3.0
    val falseWhereInError = 3.0

    val project = projectFn("Float")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong BOOLEAN = FALSE where and assign error correctly and not execute partially" in {
    val outerWhere        = true
    val innerWhere        = true
    val falseWhere        = false
    val falseWhereInError = false

    val project = projectFn("Boolean")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong BOOLEAN = TRUE where and assign error correctly and not execute partially" in {
    val outerWhere        = false
    val innerWhere        = false
    val falseWhere        = true
    val falseWhereInError = true

    val project = projectFn("Boolean")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong GRAPHQLID where and assign error correctly and not execute partially" in {
    val outerWhere        = """"Some Outer ID""""
    val innerWhere        = """"Some Inner ID""""
    val falseWhere        = """"Some False ID""""
    val falseWhereInError = "Some False ID"

    val project = projectFn("String")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong ENUM where and assign error correctly and not execute partially" in {
    val outerWhere        = "A"
    val innerWhere        = "B"
    val falseWhere        = "C"
    val falseWhereInError = "C"

    val project = projectFn("SomeEnum")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many to many relation" should "fail gracefully on wrong DateTime where and assign error correctly and not execute partially" in {
    val outerWhere        = """"2018""""
    val innerWhere        = """"2019""""
    val falseWhere        = """"2020""""
    val falseWhereInError = new DateTime("2020", DateTimeZone.UTC)

    val project = projectFn("DateTime")
    database.setup(project)

    verifyTransactionalExecutionAndErrorMessage(outerWhere, innerWhere, falseWhere, falseWhereInError, project)
  }

  "a many2many relation" should "fail gracefully on wrong GRAPHQLID for multiple nested wheres" in {
    val outerWhere         = """"Some Outer ID""""
    val innerWhere         = """"Some Inner ID""""
    val innerWhere2        = """"Some Inner ID2""""
    val falseWhere         = """"Some False ID""""
    val falseWhere2        = """"Some False ID2""""
    val falseWhereInError  = "Some False ID"
    val falseWhereInError2 = "Some False ID2"

    val project = projectFn("String")
    database.setup(project)

    val createResult = server.query(
      s"""mutation {
         |  createNote(
         |    data: {
         |      outerString: "Outer String"
         |      outerUnique: $outerWhere
         |      todos: {
         |        create: [
         |        {innerString: "Inner String", innerUnique: $innerWhere},
         |        {innerString: "Inner String", innerUnique: $innerWhere2}
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerUnique: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: [
         |        {where: { innerUnique: $innerWhere },data:{ innerString: "Changed Inner String"}},
         |        {where: { innerUnique: $falseWhere2 },data:{ innerString: "Changed Inner String"}}
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = s"No Node for the model Todo with value $falseWhereInError2 for innerUnique found."
    )

    server.query(s"""query{note(where:{outerUnique:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Outer String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere2}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerUnique: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: [
         |        {where: { innerUnique: $falseWhere},data:{ innerString: "Changed Inner String"}},
         |        {where: { innerUnique: $innerWhere2 },data:{ innerString: "Changed Inner String"}}
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = s"No Node for the model Todo with value $falseWhereInError for innerUnique found."
    )

    server.query(s"""query{note(where:{outerUnique:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Outer String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere2}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")
  }

  "a many2many relation" should "fail gracefully on wrong GRAPHQLID for multiple nested updates where one of them is not connected" in {
    val outerWhere  = """"Some Outer ID""""
    val innerWhere  = """"Some Inner ID""""
    val innerWhere2 = """"Some Inner ID2""""

    val project = projectFn("String")
    database.setup(project)

    server.query(
      s"""mutation {
         |  createNote(
         |    data: {
         |      outerString: "Outer String"
         |      outerUnique: $outerWhere
         |      todos: {
         |        create: [
         |          { innerString: "Inner String", innerUnique: $innerWhere }
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    server.query(s"""mutation {createTodo(data:{innerString: "Inner String", innerUnique: $innerWhere2}){id}}""".stripMargin, project)

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerUnique: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: [
         |          { where: { innerUnique: $innerWhere }, data:{ innerString: "Changed Inner String" } },
         |          { where: { innerUnique: $innerWhere2 },data:{ innerString: "Changed Inner String" } }
         |        ]
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3041,
      errorContains =
        s"The relation NoteToTodo has no node for the model Note connected to a Node for the model Todo with the value 'Some Inner ID2' for the field 'innerUnique' on your mutation path."
    )

    server.query(s"""query{note(where:{outerUnique:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Outer String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere2}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")
  }

  private def verifyTransactionalExecutionAndErrorMessage(outerWhere: Any, innerWhere: Any, falseWhere: Any, falseWhereInError: Any, project: Project) = {
    server.query(
      s"""mutation {
         |  createNote(
         |    data: {
         |      outerString: "Outer String"
         |      outerUnique: $outerWhere
         |      todos: {
         |        create: {
         |         innerString: "Inner String"
         |         innerUnique: $innerWhere
         |        }
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}""".stripMargin,
      project
    )

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerUnique: $outerWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: {
         |          where: { innerUnique: $falseWhere },
         |          data:{ innerString: "Changed Inner String" }
         |        }
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = s"No Node for the model Todo with value $falseWhereInError for innerUnique found."
    )

    server.query(s"""query{note(where:{outerUnique:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Outer String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateNote(
         |    where: { outerUnique: $falseWhere }
         |    data: {
         |      outerString: "Changed Outer String"
         |      todos: {
         |        update: {
         |          where: { innerUnique: $innerWhere },
         |          data:{ innerString: "Changed Inner String" }
         |        }
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project,
      errorCode = 3039,
      errorContains = s"No Node for the model Note with value $falseWhereInError for outerUnique found."
    )

    server.query(s"""query{note(where:{outerUnique:$outerWhere}){outerString}}""", project, dataContains = s"""{"note":{"outerString":"Outer String"}}""")
    server.query(s"""query{todo(where:{innerUnique:$innerWhere}){innerString}}""", project, dataContains = s"""{"todo":{"innerString":"Inner String"}}""")
  }
}
