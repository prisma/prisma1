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
      .field("unicorn", _.String, isUnique = true)
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncate(project)

  "A Delete Mutation" should "delete and return item" in {
    val id =
      server.executeQuerySimple(s"""mutation {createScalarModel(data: {string: "test"}){id}}""", project = project).pathAsString("data.createScalarModel.id")
    server.executeQuerySimple(s"""mutation {deleteScalarModel(where: {id: "$id"}){id}}""",
                              project = project,
                              dataContains = s"""{"deleteScalarModel":{"id":"$id"}""")
    server.executeQuerySimple(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[]}""")
  }

  "A Delete Mutation" should "gracefully fail on non-existing id" in {
    val id =
      server.executeQuerySimple(s"""mutation {createScalarModel(data: {string: "test"}){id}}""", project = project).pathAsString("data.createScalarModel.id")
    server.executeQuerySimpleThatMustFail(
      s"""mutation {deleteScalarModel(where: {id: "DOES NOT EXIST"}){id}}""",
      project = project,
      errorCode = 3039,
      errorContains = "No Node for the model ScalarModel with value DOES NOT EXIST for id found"
    )
    server.executeQuerySimple(s"""query {scalarModels{string}}""", project = project, dataContains = s"""{"scalarModels":[{"string":"test"}]}""")
  }

  "A Delete Mutation" should "delete and return item on non id unique field" in {
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {unicorn: "a"}){id}}""", project = project)
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {unicorn: "b"}){id}}""", project = project)
    server.executeQuerySimple(s"""mutation {deleteScalarModel(where: {unicorn: "a"}){unicorn}}""",
                              project = project,
                              dataContains = s"""{"deleteScalarModel":{"unicorn":"a"}""")
    server.executeQuerySimple(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[{"unicorn":"b"}]}""")
  }

  "A Delete Mutation" should "gracefully fail when trying to delete on non-existent value for non id unique field" in {
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {unicorn: "a"}){id}}""", project = project)
    server.executeQuerySimpleThatMustFail(
      s"""mutation {deleteScalarModel(where: {unicorn: "c"}){unicorn}}""",
      project = project,
      errorCode = 3039,
      errorContains = "No Node for the model ScalarModel with value c for unicorn found"
    )
    server.executeQuerySimple(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[{"unicorn":"a"}]}""")
  }

  "A Delete Mutation" should "gracefully fail when referring to a non-unique field" in {
    server.executeQuerySimple(s"""mutation {createScalarModel(data: {string: "a"}){id}}""", project = project)
    server.executeQuerySimpleThatMustFail(
      s"""mutation {deleteScalarModel(where: {string: "a"}){string}}""",
      project = project,
      errorCode = 0,
      errorContains = s"""Argument 'where' expected type 'ScalarModelWhereUniqueInput!' but got: {string: \\"a\\"}"""
    )
    server.executeQuerySimple(s"""query {scalarModels{string}}""", project = project, dataContains = s"""{"scalarModels":[{"string":"a"}]}""")
  }
}
