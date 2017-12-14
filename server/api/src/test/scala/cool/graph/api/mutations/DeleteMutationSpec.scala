package cool.graph.api.mutations

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.models.Project
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteMutationSpec extends FlatSpec with Matchers with ApiBaseSpec {

  val project: Project = SchemaDsl() { schema =>
    schema
      .model("ScalarModel")
      .field("string", _.String)
      .field("unique", _.String, isUnique = true)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = {
    database.truncate(project)
  }

  "A Delete Mutation" should "delete and return item" in {
    val id = server.executeQuerySimple(s"""mutation {createScalarModel(data: {string: "test"}){id}}""", project = project).pathAsString("data.createScalarModel.id")
    server.executeQuerySimple(s"""mutation {deleteScalarModel(where: {id: "$id"}){id}}""", project = project, dataContains = s"""{"deleteScalarModel":{"id":"$id"}""")
  }

  "A Delete Mutation" should "delete and return item on non id unique field" in {
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {unique: "a"}){id}}""", project = project)
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {unique: "b"}){id}}""", project = project)
    server.executeQuerySimple(s"""mutation {deleteScalarModel(where: {unique: "a"}){unique}}""", project = project, dataContains = s"""{"deleteScalarModel":{"unique":"a"}""")
  }

  "A Delete Mutation" should "gracefully fail when referring to a non-unique field" in {
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {string: "a"}){id}}""", project = project)
    server.executeQuerySimpleThatMustFail(s"""mutation {deleteScalarModel(where: {string: "a"}){string}}""", project = project, errorCode = 0,
      errorContains = s"""Argument 'where' expected type 'ScalarModelWhereUniqueInput!' but got: {string: \\"a\\"}""")
  }
}
