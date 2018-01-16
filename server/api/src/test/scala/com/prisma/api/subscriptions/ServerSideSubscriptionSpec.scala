package com.prisma.api.subscriptions

import com.prisma.api.ApiBaseSpec
import com.prisma.shared.models._
import com.prisma.shared.project_dsl.SchemaDsl
import com.prisma.shared.project_dsl.SchemaDsl.ModelBuilder
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

class ServerSideSubscriptionSpec extends FlatSpec with Matchers with ApiBaseSpec with ScalaFutures {
  import spray.json._

  val webhookTestKit = testDependencies.webhookPublisher

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(actualProject)
  }

  override def beforeEach = {
    super.beforeEach()

    database.truncate(project)
    webhookTestKit.reset
  }

  val project = SchemaDsl.schema() { schema =>
    val status: Enum = schema.enum("TodoStatus", Vector("Active", "Done"))
    val comment: ModelBuilder = schema
      .model("Comment")
      .field("text", _.String)
    val todo: ModelBuilder = schema
      .model("Todo")
      .field("title", _.String)
      .field("status", _.Enum, enum = Some(status))
      .oneToManyRelation("comments", "todo", comment)
  }

  val subscriptionQueryForCreates: String =
    """
      |subscription {
      |  todo(where: {
      |    mutation_in : [CREATED, UPDATED, DELETED]
      |    node: {
      |      status: Active
      |    }
      |  }){
      |    node {
      |      title
      |      status
      |      comments {
      |        text
      |      }
      |    }
      |    previousValues {
      |      title
      |    }
      |  }
      |}
    """.stripMargin

  val webhookUrl     = "http://www.mywebhooks.com"
  val webhookHeaders = Vector("header" -> "value")
  val sssFunction = ServerSideSubscriptionFunction(
    name = "Test Function",
    isActive = true,
    query = subscriptionQueryForCreates,
    delivery = WebhookDelivery(
      url = webhookUrl,
      headers = webhookHeaders
    )
  )

  val actualProject: Project = project.copy(functions = List(sssFunction))

  val newTodoTitle     = "The title of the new todo"
  val newTodoStatus    = "Active"
  val updatedTodoTitle = "The title of the updated todo"

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches on a Create" in {
    val createTodo =
      s"""
         |mutation {
         |  createTodo(data:{
         |    title:"$newTodoTitle"
         |    status: $newTodoStatus
         |  }){
         |    id
         |  }
         |}
      """.stripMargin
    val id = server.executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunction.name
    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl

    webhook.payload shouldEqual s"""|{
                                    |  "data": {
                                    |    "todo": {
                                    |      "node": {
                                    |        "title": "$newTodoTitle",
                                    |        "status": "$newTodoStatus",
                                    |        "comments": []
                                    |      },
                                    |      "previousValues": null
                                    |    }
                                    |  }
                                    |}""".stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches on an Update" in {
    val createTodo =
      s"""
         |mutation {
         |  createTodo(data: {
         |    title:"$newTodoTitle"
         |  }){
         |    id
         |  }
         |}
      """.stripMargin
    val id = server.executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectNoPublishedMsg()

    val updateTodo =
      s"""
         |mutation {
         |  updateTodo(
         |    where: { id: "$id" }
         |    data: { title:"$updatedTodoTitle", status: Active}
         |  ){
         |    id
         |  }
         |}
      """.stripMargin
    server.executeQuerySimple(updateTodo, actualProject).pathAsString("data.updateTodo.id")

    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunction.name
    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl
    webhook.payload shouldEqual s"""{
                                    |  "data": {
                                    |    "todo": {
                                    |      "node": {
                                    |        "title": "$updatedTodoTitle",
                                    |        "status": "Active",
                                    |        "comments": []
                                    |      },
                                    |      "previousValues": {
                                    |        "title": "$newTodoTitle"
                                    |      }
                                    |    }
                                    |  }
                                    |}""".stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches on an Delete" in {
    val createTodo =
      s"""
         |mutation {
         |  createTodo(data: {
         |    title:"$newTodoTitle"
         |  }){
         |    id
         |  }
         |}
      """.stripMargin

    val id = server.executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectNoPublishedMsg()

    val deleteTodo =
      s"""
         |mutation {
         |  deleteTodo(where: {
         |    id: "$id"
         |  }){
         |    id
         |  }
         |}
      """.stripMargin

    server.executeQuerySimple(deleteTodo, actualProject).pathAsString("data.deleteTodo.id")
    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunction.name
    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl

    webhook.payload shouldEqual s"""
                                  |{
                                  |  "data": {
                                  |    "todo": {
                                  |      "node": null,
                                  |      "previousValues": {
                                  |        "title": "$newTodoTitle"
                                  |      }
                                  |    }
                                  |  }
                                  |}""".stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches a nested Create mutation" in {
    val theTitle = "The title of the new todo"
    val createCommentWithNestedTodo =
      s"""
         |mutation {
         |  createComment(data: {
         |    text:"some text",
         |    todo: {
         |      create: {
         |        title:"$theTitle"
         |        status: $newTodoStatus
         |      }
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin

    server.executeQuerySimple(createCommentWithNestedTodo, actualProject).pathAsString("data.createComment.id")
    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunction.name
    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl

    webhook.payload shouldEqual s"""
                                    |{
                                    |  "data": {
                                    |    "todo": {
                                    |      "node": {
                                    |        "title": "$newTodoTitle",
                                    |        "status": "$newTodoStatus",
                                    |        "comments": [{"text":"some text"}]
                                    |      },
                                    |      "previousValues": null
                                    |    }
                                    |  }
                                    |}""".stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches a nested Update mutation" in {
    val newTodoTitle = "The title of the new todo"
    val createComment =
      s"""
         |mutation {
         |  createComment(data:{
         |    text:"some text"
         |  }){
         |    id
         |  }
         |}
      """.stripMargin
    val commentId = server.executeQuerySimple(createComment, actualProject).pathAsString("data.createComment.id")

    webhookTestKit.expectNoPublishedMsg()

    val updateCommentWithNestedTodo =
      s"""
         |mutation {
         |  updateComment(
         |    where: { id: "$commentId"}
         |    data: {
         |      text:"some updated text"
         |      todo: {
         |        create: {
         |          title:"$newTodoTitle"
         |          status: $newTodoStatus
         |        }
         |      }
         |    }
         |  ){
         |    id
         |  }
         |}
       """.stripMargin

    server.executeQuerySimple(updateCommentWithNestedTodo, actualProject).pathAsString("data.updateComment.id")

    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunction.name
    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl
    webhook.payload shouldEqual s"""
                                  |{
                                  |  "data": {
                                  |    "todo": {
                                  |      "node": {
                                  |        "title": "$newTodoTitle",
                                  |        "status": "$newTodoStatus",
                                  |        "comments": [{"text":"some updated text"}]
                                  |      },
                                  |      "previousValues": null
                                  |    }
                                  |  }
                                  |}""".stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

  "ServerSideSubscription" should "NOT send a message to our Webhook Queue if the SSS Query does not match" in {
    val theTitle = "The title of the new todo"
    val createTodo =
      s"""mutation {
         |  createTodo(data:{
         |    title:"$theTitle"
         |    status: Active
         |  }){
         |    id
         |  }
         |}
      """.stripMargin
    val id = server.executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectPublishCount(1)

    server
      .executeQuerySimple(
        s"""
         |mutation {
         |  updateTodo(
         |    where: { id: "$id" }
         |    data: { title:"new title", status: Done } 
         |  ){
         |    id
         |  }
         |}
      """.stripMargin,
        actualProject
      )
      .pathAsString("data.updateTodo.id")

    webhookTestKit.expectNoPublishedMsg()
  }
}
