package com.prisma.subscriptions

import com.prisma.ConnectorTag
import com.prisma.ConnectorTag.SQLiteConnectorTag
import com.prisma.api.ApiSpecBase
import com.prisma.shared.models.ConnectorCapability.JoinRelationLinksCapability
import com.prisma.shared.models._
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{FlatSpec, Matchers}

class NonEmbeddedServerSideSubscriptionSpec extends FlatSpec with Matchers with ApiSpecBase with ScalaFutures {

  override def runOnlyForCapabilities: Set[ConnectorCapability] = Set(JoinRelationLinksCapability)
  override def doNotRunForConnectors: Set[ConnectorTag]         = Set(SQLiteConnectorTag)

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

  val project = SchemaDsl.fromStringV11() {
    """
      |type Todo {
      |   id: ID! @id
      |   title: String
      |   status: TodoStatus
      |   comments: [Comment]
      |}
      |
      |type Comment{
      |   id: ID! @id
      |   text: String
      |   todo: Todo @relation(link: INLINE)
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
      |    ${if (mutation == "UPDATED") """updatedFields_contains: "title"""" else ""}
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

  val actualProject: Project = project.copy(functions = List(sssFunctionForCreate, sssFunctionForUpdate, sssFunctionForDeleted))

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

    server.query(createCommentWithNestedTodo, actualProject).pathAsString("data.createComment.id")
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
                                    |        "comments": [{"text":"some text"}]
                                    |      },
                                    |      "previousValues": null
                                    |    }
                                    |  }
                                    |}""".stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches a nested Create in an Update mutation" in {
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
    val commentId = server.query(createComment, actualProject).pathAsString("data.createComment.id")

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

    server.query(updateCommentWithNestedTodo, actualProject).pathAsString("data.updateComment.id")

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
