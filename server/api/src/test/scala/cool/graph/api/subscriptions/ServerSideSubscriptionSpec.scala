package cool.graph.api.subscriptions

import cool.graph.api.ApiBaseSpec
import cool.graph.shared.models._
import cool.graph.shared.project_dsl.SchemaDsl
import cool.graph.shared.project_dsl.SchemaDsl.ModelBuilder
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
      |  Todo(filter: {
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
    delivery = WebhookFunction(
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
         |  createTodo(title:"$newTodoTitle", status: $newTodoStatus){
         |    id
         |  }
         |}
      """.stripMargin
    val id = server.executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")

    webhookTestKit.expectPublishCount(1)

    val webhook = webhookTestKit.messagesPublished.head

    webhook.functionName shouldEqual sssFunction.name
    webhook.projectId shouldEqual project.id
    webhook.requestId shouldNot be(empty)
    webhook.id shouldNot be(empty)
    webhook.url shouldEqual webhookUrl

    webhook.payload shouldEqual s"""
                                                |{
                                                |  "data": {
                                                |    "Todo": {
                                                |      "node": {
                                                |        "title": "$newTodoTitle",
                                                |        "status": "$newTodoStatus",
                                                |        "comments": []
                                                |      },
                                                |      "previousValues": null
                                                |    }
                                                |  }
                                                |}
      """.stripMargin.parseJson.compactPrint

    webhook.headers shouldEqual Map("header" -> "value")
  }

//  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches on an Update" in {
//    val createTodo =
//      s"""
//         |mutation {
//         |  createTodo(title:"$newTodoTitle"){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//    val id = executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")
//
//    webhookTestKit.expectNoPublishedMsg()
//
//    val updateTodo =
//      s"""
//         |mutation {
//         |  updateTodo(id: "$id", title:"$updatedTodoTitle", status: Active){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//    val _ = executeQuerySimple(updateTodo, actualProject).pathAsString("data.updateTodo.id")
//
//    webhookTestKit.expectPublishCount(1)
//
//    val webhook = webhookTestKit.messagesPublished.head
//
//    webhook.functionName shouldEqual sssFunction.id
//    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
//    webhook.url shouldEqual webhookUrl
//    webhook.payload.redactTokens shouldEqual s"""
//                                                |{
//                                                |  "data": {
//                                                |    "Todo": {
//                                                |      "node": {
//                                                |        "title": "$updatedTodoTitle",
//                                                |        "status": "Active",
//                                                |        "comments": []
//                                                |      },
//                                                |      "previousValues": {
//                                                |        "title": "$newTodoTitle"
//                                                |      }
//                                                |    }
//                                                |  },
//                                                |  "context": {
//                                                |    "request": {
//                                                |      "sourceIp": "",
//                                                |      "headers": {
//                                                |
//                                              |      },
//                                                |      "httpMethod": "post"
//                                                |    },
//                                                |    "auth": null,
//                                                |    "sessionCache": {
//                                                |
//                                              |    },
//                                                |    "environment": {
//                                                |
//                                              |    },
//                                                |    "graphcool": {
//                                                |      "projectId": "test-project-id",
//                                                |      "alias": "test-project-alias",
//                                                |      "pat": "*",
//                                                |      "serviceId":"test-project-id",
//                                                |      "rootToken": "*",
//                                                |      "endpoints": $endpoints
//                                                |    }
//                                                |  }
//                                                |}""".stripMargin.parseJson.compactPrint
//
//    webhook.headers shouldEqual Map("header" -> "value")
//  }
//
//  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches on an Delete" in {
//    val createTodo =
//      s"""
//         |mutation {
//         |  createTodo(title:"$newTodoTitle"){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//
//    val id = executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")
//
//    webhookTestKit.expectNoPublishedMsg()
//
//    val updateTodo =
//      s"""
//         |mutation {
//         |  deleteTodo(id: "$id"){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//
//    executeQuerySimple(updateTodo, actualProject).pathAsString("data.deleteTodo.id")
//    webhookTestKit.expectPublishCount(1)
//
//    val webhook = webhookTestKit.messagesPublished.head
//
//    webhook.functionName shouldEqual sssFunction.id
//    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
//    webhook.url shouldEqual webhookUrl
//
//    webhook.payload.redactTokens shouldEqual s"""
//                                                |{
//                                                |  "data": {
//                                                |    "Todo": {
//                                                |      "node": null,
//                                                |      "previousValues": {
//                                                |        "title": "$newTodoTitle"
//                                                |      }
//                                                |    }
//                                                |  },
//                                                |  "context": {
//                                                |    "request": {
//                                                |      "sourceIp": "",
//                                                |      "headers": {
//                                                |
//                                              |      },
//                                                |      "httpMethod": "post"
//                                                |    },
//                                                |    "auth": null,
//                                                |    "sessionCache": {
//                                                |
//                                              |    },
//                                                |    "environment": {
//                                                |
//                                              |    },
//                                                |    "graphcool": {
//                                                |      "projectId": "test-project-id",
//                                                |      "alias": "test-project-alias",
//                                                |      "pat": "*",
//                                                |      "serviceId":"test-project-id",
//                                                |      "rootToken": "*",
//                                                |      "endpoints": $endpoints
//                                                |    }
//                                                |  }
//                                                |}""".stripMargin.parseJson.compactPrint
//
//    webhook.headers shouldEqual Map("header" -> "value")
//  }
//
//  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches a nested Create mutation" in {
//    val theTitle = "The title of the new todo"
//    val createCommentWithNestedTodo =
//      s"""
//         |mutation {
//         |  createComment(text:"some text", todo: {
//         |    title:"$theTitle"
//         |    status: $newTodoStatus
//         |  }){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//
//    executeQuerySimple(createCommentWithNestedTodo, actualProject).pathAsString("data.createComment.id")
//    webhookTestKit.expectPublishCount(1)
//
//    val webhook = webhookTestKit.messagesPublished.head
//
//    webhook.functionName shouldEqual sssFunction.id
//    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
//    webhook.url shouldEqual webhookUrl
//
//    webhook.payload.redactTokens shouldEqual s"""
//                                                |{
//                                                |  "data": {
//                                                |    "Todo": {
//                                                |      "node": {
//                                                |        "title": "$newTodoTitle",
//                                                |        "status": "$newTodoStatus",
//                                                |        "comments": [{"text":"some text"}]
//                                                |      },
//                                                |      "previousValues": null
//                                                |    }
//                                                |  },
//                                                |  "context": {
//                                                |    "request": {
//                                                |      "sourceIp": "",
//                                                |      "headers": {
//                                                |
//                                              |      },
//                                                |      "httpMethod": "post"
//                                                |    },
//                                                |    "auth": null,
//                                                |    "sessionCache": {
//                                                |
//                                              |    },
//                                                |    "environment": {
//                                                |
//                                              |    },
//                                                |    "graphcool": {
//                                                |      "projectId": "test-project-id",
//                                                |      "alias": "test-project-alias",
//                                                |      "pat": "*",
//                                                |      "serviceId":"test-project-id",
//                                                |      "rootToken": "*",
//                                                |      "endpoints": $endpoints
//                                                |    }
//                                                |  }
//                                                |}""".stripMargin.parseJson.compactPrint
//
//    webhook.headers shouldEqual Map("header" -> "value")
//  }
//
//  "ServerSideSubscription" should "send a message to our Webhook Queue if the SSS Query matches a nested Update mutation" in {
//    val newTodoTitle = "The title of the new todo"
//    val createComment =
//      s"""
//         |mutation {
//         |  createComment(text:"some text"){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//    val commentId = executeQuerySimple(createComment, actualProject).pathAsString("data.createComment.id")
//
//    webhookTestKit.expectNoPublishedMsg()
//
//    val updateCommentWithNestedTodo =
//      s"""
//         |mutation {
//         |  updateComment(id: "$commentId",text:"some updated text", todo: {
//         |    title:"$newTodoTitle"
//         |    status: $newTodoStatus
//         |  }){
//         |    id
//         |  }
//         |}
//       """.stripMargin
//
//    val _ = executeQuerySimple(updateCommentWithNestedTodo, actualProject).pathAsString("data.updateComment.id")
//
//    webhookTestKit.expectPublishCount(1)
//
//    val webhook = webhookTestKit.messagesPublished.head
//
//    webhook.functionName shouldEqual sssFunction.id
//    webhook.projectId shouldEqual project.id
//    webhook.requestId shouldNot be(empty)
//    webhook.id shouldNot be(empty)
//    webhook.url shouldEqual webhookUrl
//    webhook.payload.redactTokens shouldEqual s"""
//                                                |{
//                                                |  "data": {
//                                                |    "Todo": {
//                                                |      "node": {
//                                                |        "title": "$newTodoTitle",
//                                                |        "status": "$newTodoStatus",
//                                                |        "comments": [{"text":"some updated text"}]
//                                                |      },
//                                                |      "previousValues": null
//                                                |    }
//                                                |  },
//                                                |  "context": {
//                                                |    "request": {
//                                                |      "sourceIp": "",
//                                                |      "headers": {
//                                                |
//                                                |      },
//                                                |      "httpMethod": "post"
//                                                |    },
//                                                |    "auth": null,
//                                                |    "sessionCache": {
//                                                |
//                                                |    },
//                                                |    "environment": {
//                                                |
//                                                |    },
//                                                |    "graphcool": {
//                                                |      "projectId": "test-project-id",
//                                                |      "alias": "test-project-alias",
//                                                |      "pat": "*",
//                                                |      "serviceId":"test-project-id",
//                                                |      "rootToken": "*",
//                                                |      "endpoints": $endpoints
//                                                |    }
//                                                |  }
//                                                |}""".stripMargin.parseJson.compactPrint
//
//    webhook.headers shouldEqual Map("header" -> "value")
//  }
//
//  "ServerSideSubscription" should "NOT send a message to our Webhook Queue if the SSS Query does not match" in {
//    val theTitle = "The title of the new todo"
//    val createTodo =
//      s"""
//         |mutation {
//         |  createTodo(title:"$theTitle", status: Active){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//    val id = executeQuerySimple(createTodo, actualProject).pathAsString("data.createTodo.id")
//
//    webhookTestKit.expectPublishCount(1)
//
//    executeQuerySimple(
//      s"""
//         |mutation {
//         |  updateTodo(id: "$id", title:"new title", status: Done){
//         |    id
//         |  }
//         |}
//      """.stripMargin,
//      actualProject
//    ).pathAsString("data.updateTodo.id")
//
//    webhookTestKit.expectNoPublishedMsg()
//  }
//
//  "ServerSideSubscription" should "trigger a managed function" in {
//    val actualProjectManagedFunction = project.copy(functions = List(sssManagedFunction))
//    def endpoints                    = AnyJsonFormat.write(endpointResolver.endpoints(actualProjectManagedFunction.id).toMap).compactPrint
//
//    val createTodo =
//      s"""
//         |mutation {
//         |  createTodo(title:"$newTodoTitle", status: $newTodoStatus){
//         |    id
//         |  }
//         |}
//      """.stripMargin
//
//    executeQuerySimple(createTodo, actualProjectManagedFunction).pathAsString("data.createTodo.id")
//    val functionEnvironment = injector.functionEnvironment.asInstanceOf[TestFunctionEnvironment]
//    val invocations         = functionEnvironment.invocations
//
//    invocations.length shouldEqual 1      // Fire one managed function
//    webhookTestKit.expectNoPublishedMsg() // Don't fire a webhook
//
//    val lastInvocation = invocations.last
//    val parsedEvent    = lastInvocation.event.parseJson
//
//    lastInvocation.event.redactTokens shouldEqual s"""
//                                                     |{
//                                                     |  "data": {
//                                                     |    "Todo": {
//                                                     |      "node": {
//                                                     |        "title": "$newTodoTitle",
//                                                     |        "status": "$newTodoStatus",
//                                                     |        "comments": []
//                                                     |      },
//                                                     |      "previousValues": null
//                                                     |    }
//                                                     |  },
//                                                     |  "context": {
//                                                     |    "request": {
//                                                     |      "sourceIp": "",
//                                                     |      "headers": {
//                                                     |
//       |      },
//                                                     |      "httpMethod": "post"
//                                                     |    },
//                                                     |    "auth": null,
//                                                     |    "sessionCache": {
//                                                     |
//       |    },
//                                                     |    "environment": {
//                                                     |
//       |    },
//                                                     |    "graphcool": {
//                                                     |      "projectId": "test-project-id",
//                                                     |      "alias": "test-project-alias",
//                                                     |      "pat": "*",
//                                                     |      "serviceId":"test-project-id",
//                                                     |      "rootToken": "*",
//                                                     |      "endpoints": $endpoints
//                                                     |    }
//                                                     |  }
//                                                     |}
//      """.stripMargin.parseJson.compactPrint
//  }
}
