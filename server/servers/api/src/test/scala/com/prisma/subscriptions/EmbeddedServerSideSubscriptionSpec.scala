package com.prisma.subscriptions

import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.EmbeddedTypesCapability
import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

class EmbeddedServerSideSubscriptionSpec extends FlatSpec with Matchers with ApiSpecBase with ScalaFutures {
  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(EmbeddedTypesCapability)

  val webhookTestKit = testDependencies.webhookPublisher

  override protected def beforeAll(): Unit = {
    super.beforeAll()
    database.setup(actualProject)
  }

  override def beforeEach = {
    super.beforeEach()
    database.truncateProjectTables(project)
    webhookTestKit.reset
  }

  lazy val project = SchemaDsl.fromStringV11() {
    """
      |type Todo {
      |   id: ID! @id
      |   title: String
      |   status: TodoStatus
      |   comments: [Comment]
      |}
      |
      |type Comment @embedded{
      |   text: String
      |}
      |
      |enum TodoStatus {
      |   ACTIVE
      |   DONE
      |}
      |
    """
  }

  def subscriptionQueryFor(mutation: String): String =
    s"""
      |subscription {
      |  todo(where: {
      |    mutation_in : [$mutation]
      |    node: {
      |      status: ACTIVE
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

  def nestedSubscriptionQueryFor(mutation: String): String =
    s"""
       |subscription {
       |  comment(where: {
       |    mutation_in : [$mutation]
       |    node: {
       |      text: "test"
       |    }
       |  }){
       |    node {
       |      text
       |    }
       |    previousValues {
       |      text
       |    }
       |  }
       |}
    """.stripMargin

  val webhookUrl     = "http://www.mywebhooks.com"
  val webhookHeaders = Vector("header" -> "value")

  val sssFunctionForCreate = ServerSideSubscriptionFunction(
    name = "Test Function CREATED",
    isActive = true,
    query = subscriptionQueryFor("CREATED"),
    delivery = WebhookDelivery(
      url = webhookUrl,
      headers = webhookHeaders
    )
  )

  val sssFunctionForUpdate = ServerSideSubscriptionFunction(
    name = "Test Function UPDATED",
    isActive = true,
    query = subscriptionQueryFor("UPDATED"),
    delivery = WebhookDelivery(
      url = webhookUrl,
      headers = webhookHeaders
    )
  )

  val sssFunctionForDeleted = ServerSideSubscriptionFunction(
    name = "Test Function DELETED",
    isActive = true,
    query = subscriptionQueryFor("DELETED"),
    delivery = WebhookDelivery(
      url = webhookUrl,
      headers = webhookHeaders
    )
  )

  lazy val actualProject: Project = project.copy(functions = List(sssFunctionForCreate, sssFunctionForUpdate, sssFunctionForDeleted))

  val newTodoTitle     = "The title of the new todo"
  val newTodoStatus    = "ACTIVE"
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

    val id = server.query(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunctionForCreate.name
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
    val id = server.query(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectNoPublishedMsg()

    val updateTodo =
      s"""
         |mutation {
         |  updateTodo(
         |    where: { id: "$id" }
         |    data: { title:"$updatedTodoTitle", status: ACTIVE}
         |  ){
         |    id
         |  }
         |}
      """.stripMargin
    server.query(updateTodo, actualProject).pathAsString("data.updateTodo.id")

    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunctionForUpdate.name
    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl
    webhook.payload shouldEqual s"""{
                                    |  "data": {
                                    |    "todo": {
                                    |      "node": {
                                    |        "title": "$updatedTodoTitle",
                                    |        "status": "ACTIVE",
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

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches on a Delete" in {
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

    val id = server.query(createTodo, actualProject).pathAsString("data.createTodo.id")

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

    server.query(deleteTodo, actualProject).pathAsString("data.deleteTodo.id")
    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunctionForDeleted.name
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

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches a nested Create inside a Create mutation" in {
    val theText = "test"
    val createCommentWithNestedTodo =
      s"""mutation {
         |  createTodo(data: {
         |    title:"$newTodoTitle",
         |    status: $newTodoStatus
         |    comments: {
         |      create: {
         |        text:"$theText"
         |      }
         |    }
         |  }){
         |    id
         |  }
         |}
      """.stripMargin

    server.query(createCommentWithNestedTodo, actualProject).pathAsString("data.createTodo.id")
    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunctionForCreate.name
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
                                    |        "comments": [{"text":"$theText"}]
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
         |    status: ACTIVE
         |  }){
         |    id
         |  }
         |}
      """.stripMargin

    val id = server.query(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectPublishCount(1)

    server
      .query(
        s"""
         |mutation {
         |  updateTodo(
         |    where: { id: "$id" }
         |    data: { title:"new title", status: DONE } 
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
