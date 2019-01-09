package com.prisma.api.mutations

import com.prisma.api.ApiSpecBase
import com.prisma.api.util.TroubleCharacters
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}

class UpdateMutationSpec extends FlatSpec with Matchers with ApiSpecBase {

  "The Update Mutation" should "update an item" in {
    val project = SchemaDsl.fromBuilder { schema =>
      val enum = schema.enum(
        name = "MyEnum",
        values = Vector(
          "A",
          "ABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJABCDEFGHIJ"
        )
      )
      schema
        .model("ScalarModel")
        .field("optString", _.String)
        .field("optInt", _.Int)
        .field("optFloat", _.Float)
        .field("optBoolean", _.Boolean)
        .field("optEnum", _.Enum, enum = Some(enum))
        .field("optDateTime", _.DateTime)
        .field("optJson", _.Json)
    }
    database.setup(project)

    val createResult = server.query(
      """mutation {
        |  createScalarModel(data: {
        |  })
        |  { id }
        |}""".stripMargin,
      project = project
    )
    val id = createResult.pathAsString("data.createScalarModel.id")

    val updateResult = server.query(
      s"""
        |mutation {
        |  updateScalarModel(
        |    data:{
        |      optString: "lala${TroubleCharacters.value}", optInt: 1337, optFloat: 1.234, optBoolean: true, optEnum: A, optDateTime: "2016-07-31T23:59:01.000Z", optJson: "[1,2,3]"
        |    }
        |    where: {
        |      id: "$id"
        |    }
        |  ){
        |    optString, optInt, optFloat, optBoolean, optEnum, optDateTime, optJson
        |  }
        |}
      """.stripMargin,
      project
    )

    updateResult.pathAsJsValue("data.updateScalarModel").toString should be(
      s"""{"optJson":[1,2,3],"optInt":1337,"optBoolean":true,"optDateTime":"2016-07-31T23:59:01.000Z","optString":"lala${TroubleCharacters.value}","optEnum":"A","optFloat":1.234}""")

    val query = server.query(
      s"""
         |{
         |  scalarModels {
         |    id
         |  }
         |}
       """.stripMargin,
      project
    )
    query.pathAsJsValue("data.scalarModels").toString should equal(s"""[{"id":"$id"}]""")
  }

  "The Update Mutation" should "update an item by a unique field" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }
    database.setup(project)

    val alias = "the-alias"
    server.query(
      s"""
        |mutation {
        |  createTodo(
        |    data: {
        |      title: "initial title", alias: "$alias"
        |    }
        |  ){
        |    id
        |  }
        |}
      """.stripMargin,
      project
    )

    val updateResult = server.query(
      s"""
        |mutation {
        |  updateTodo(
        |    data: {
        |      title: "updated title"
        |    }
        |    where: {
        |      alias: "$alias"
        |    }
        |  ){
        |    title
        |  }
        |}""".stripMargin,
      project
    )
    updateResult.pathAsString("data.updateTodo.title") should equal("updated title")
  }

  "The Update Mutation" should "gracefully fail when trying to update an item by a unique field with a non-existing value" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field_!("title", _.String).field("alias", _.String, isUnique = true)
    }
    database.setup(project)

    val alias = "the-alias"
    server.query(
      s"""
         |mutation {
         |  createTodo(
         |    data: {
         |      title: "initial title", alias: "$alias"
         |    }
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
      project
    )

    server.queryThatMustFail(
      s"""
         |mutation {
         |  updateTodo(
         |    data: {
         |      title: "updated title"
         |    }
         |    where: {
         |      alias: "NOT A VALID ALIAS"
         |    }
         |  ){
         |    title
         |  }
         |}""".stripMargin,
      project,
      errorCode = 3039,
      errorContains = "No Node for the model Todo with value NOT A VALID ALIAS for alias found"
    )
  }

  "Updating" should "change the updatedAt datetime" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema
        .model("Todo")
        .field("title", _.String)
        .field("text", _.String)
        .field("alias", _.String, isUnique = true)
        .field("createdAt", _.DateTime)
        .field("updatedAt", _.DateTime)
    }
    database.setup(project)

    val alias = "the-alias"
    server.query(
      s"""
         |mutation {
         |  createTodo(
         |    data: {
         |      title: "initial title",
         |      text: "some text"
         |      alias: "$alias"
         |    }
         |  ){
         |    createdAt
         |    updatedAt
         |  }
         |}
      """,
      project
    )

    Thread.sleep(1000)

    val res = server.query(
      s"""
         |mutation {
         |  updateTodo(
         |    data: {
         |      title: null
         |    }
         |    where: {
         |      alias: "$alias"
         |    }
         |  ){
         |    createdAt
         |    updatedAt
         |  }
         |}""",
      project
    )

    val createdAt = res.pathAsString("data.updateTodo.createdAt")
    val updatedAt = res.pathAsString("data.updateTodo.updatedAt")

    createdAt should not be updatedAt
  }
}
