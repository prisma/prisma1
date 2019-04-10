package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class DeleteMutationSpec extends FlatSpec with Matchers with ApiSpecBase {

  val project = SchemaDsl.fromStringV11() {
    """
      |type ScalarModel {
      |  id: ID! @id
      |  string: String
      |  unicorn: String @unique
      |}
    """.stripMargin
  }

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(project)
  }

  override def beforeEach(): Unit = database.truncateProjectTables(project)

  "A Delete Mutation" should "delete and return item" in {
    val id =
      server.query(s"""mutation {createScalarModel(data: {string: "test"}){id}}""", project = project).pathAsString("data.createScalarModel.id")
    server.query(s"""mutation {deleteScalarModel(where: {id: "$id"}){id}}""", project = project, dataContains = s"""{"deleteScalarModel":{"id":"$id"}""")
    server.query(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[]}""")
  }

  "A Delete Mutation" should "gracefully fail on non-existing id" in {
    val id =
      server.query(s"""mutation {createScalarModel(data: {string: "test"}){id}}""", project = project).pathAsString("data.createScalarModel.id")
    server.queryThatMustFail(
      s"""mutation {deleteScalarModel(where: {id: "5beea4aa6183dd734b2dbd9b"}){id}}""",
      project = project,
      errorCode = 3039,
      errorContains = "No Node for the model ScalarModel with value 5beea4aa6183dd734b2dbd9b for id found"
    )
    server.query(s"""query {scalarModels{string}}""", project = project, dataContains = s"""{"scalarModels":[{"string":"test"}]}""")
  }

  "A Delete Mutation" should "delete and return item on non id unique field" in {
    server.query(s"""mutation {createScalarModel(data: {unicorn: "a"}){id}}""", project = project)
    server.query(s"""mutation {createScalarModel(data: {unicorn: "b"}){id}}""", project = project)
    server.query(s"""mutation {deleteScalarModel(where: {unicorn: "a"}){unicorn}}""",
                 project = project,
                 dataContains = s"""{"deleteScalarModel":{"unicorn":"a"}""")
    server.query(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[{"unicorn":"b"}]}""")
  }

  "A Delete Mutation" should "gracefully fail when trying to delete on non-existent value for non id unique field" in {
    server.query(s"""mutation {createScalarModel(data: {unicorn: "a"}){id}}""", project = project)
    server.queryThatMustFail(
      s"""mutation {deleteScalarModel(where: {unicorn: "c"}){unicorn}}""",
      project = project,
      errorCode = 3039,
      errorContains = "No Node for the model ScalarModel with value c for unicorn found"
    )
    server.query(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[{"unicorn":"a"}]}""")
  }

  "A Delete Mutation" should "gracefully fail when trying to delete on null value for unique field" in {
    server.query(s"""mutation {createScalarModel(data: {unicorn: "a"}){id}}""", project = project)
    server.queryThatMustFail(
      s"""mutation {deleteScalarModel(where: {unicorn: null}){unicorn}}""",
      project = project,
      errorCode = 3040,
      errorContains = "You provided an invalid argument for the where selector on ScalarModel."
    )
    server.query(s"""query {scalarModels{unicorn}}""", project = project, dataContains = s"""{"scalarModels":[{"unicorn":"a"}]}""")
  }

  "A Delete Mutation" should "gracefully fail when referring to a non-unique field" in {
    server.query(s"""mutation {createScalarModel(data: {string: "a"}){id}}""", project = project)
    server.queryThatMustFail(
      s"""mutation {deleteScalarModel(where: {string: "a"}){string}}""",
      project = project,
      errorCode = 0,
      errorContains = s"""Argument 'where' expected type 'ScalarModelWhereUniqueInput!' but got: {string: \\"a\\"}"""
    )
    server.query(s"""query {scalarModels{string}}""", project = project, dataContains = s"""{"scalarModels":[{"string":"a"}]}""")
  }
}
