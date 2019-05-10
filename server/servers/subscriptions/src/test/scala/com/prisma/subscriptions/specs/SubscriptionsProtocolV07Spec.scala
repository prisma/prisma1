package com.prisma.subscriptions.specs

import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._
import org.scalatest.concurrent.ScalaFutures
import play.api.libs.json._

import scala.concurrent.duration._

class SubscriptionsProtocolV07Spec extends FlatSpec with Matchers with SubscriptionSpecBase with ScalaFutures {

  val project = SchemaDsl.fromStringV11() {
    """
      |type Todo {
      |  id: ID! @id
      |  text: String
      |  json: Json
      |  int: Int
      |  float: Float
      |}
    """.stripMargin
  }

  val model = project.schema.getModelByName_!("Todo")

  var testNodeId, importantTestNodeId: String = null

  override def beforeEach() = {
    super.beforeEach()
    testDatabase.setup(project)
    val json = Json.arr(1, 2, Json.obj("a" -> "b"))
    testNodeId = TestData.createTodo("some todo", json, None, project, model, testDatabase).value.toString
    importantTestNodeId = TestData.createTodo("important!", json, None, project, model, testDatabase).value.toString
  }

  "sending weird messages" should "result in a parsing error" in {
    testWebsocketV07(project) { wsClient =>
      wsClient.sendMessage("{}")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage("")
      wsClient.expectMessage(cantBeParsedError)
    }
  }

  "sending invalid start messages" should "result in an error" in {
    testInitializedWebsocket(project) { wsClient =>
      val id                = "ioPRfgqN6XMefVW6"
      val noKnownModelError = "The provided query doesn't include any known model name. Please check for the latest subscriptions API."

      // special case: also numbers have to work as subscription id
      wsClient.sendMessage(
        startMessage(id = id, query = "subscription { createPokemon { id name } }")
      )

      wsClient.expectMessage(
        errorMessage(id = id, message = noKnownModelError)
      )

      wsClient.sendMessage(
        startMessage(id = id, query = "subscription { createTodo { id text json } }")
      )

      wsClient.expectMessage(
        errorMessage(id = id, message = noKnownModelError)
      )
    }
  }

  "All subscriptions" should "support the basic subscriptions protocol" in {
    testWebsocketV07(project) { wsClient =>
      wsClient.sendMessage(connectionInit)
      wsClient.expectMessage(connectionAck)

      val id = "ioPRfgqN6XMefVW6"

      wsClient.sendMessage(startMessage(id = id, query = "subscription { todo { node { id text json } } }"))
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = id,
          payload = s"""{"todo":{"node":{"id":"$testNodeId","text":"some todo","json":[1,2,{"a":"b"}]}}}"""
        )
      )

      wsClient.sendMessage(stopMessage(id))
    }
  }

  "All subscriptions" should "support the basic subscriptions protocol with number id, null variables and operationName" in {
    testWebsocketV07(project) { wsClient =>
      wsClient.sendMessage(connectionInit)
      wsClient.expectMessage(connectionAck)

      val id = 3

      wsClient.sendMessage(startMessage(id = id, query = "subscription { todo { node { id text json } } }", variables = JsNull, operationName = None))
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = id,
          payload = s"""{"todo":{"node":{"id":"$testNodeId","text":"some todo","json":[1,2,{"a":"b"}]}}}"""
        )
      )

      wsClient.sendMessage(stopMessage(id))
    }
  }

  "Using the CREATED mutation filter" should "work" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(id = "2",
                     query = "subscription x { todo(where: {mutation_in: [CREATED]}) { node { id } } }  mutation y { createTodo { id } }",
                     operationName = "x"))
      wsClient.expectNoMessage(200.milliseconds)
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "2",
          payload = s"""{"todo":{"node":{"id":"$testNodeId"}}}"""
        )
      )
    }
  }

  "Using the DELETED mutation filter" should "work" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "3",
          operationName = "x",
          query = "subscription x { todo(where: {mutation_in: [DELETED]}) { node { id } } }  mutation y { createTodo { id } }"
        ))

      wsClient.expectNoMessage(200.milliseconds)
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:deleteTodo"),
        s"""{"nodeId":"$testNodeId","node":{"id":"$testNodeId","text":"some text"},"modelId":"${model.name}","mutationType":"DeleteNode"}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = """{"todo":{"node":null}}"""
        )
      )
    }
  }

  "Using the UPDATED mutation filter" should "work" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "4",
          query = "subscription { todo(where: {mutation_in: [UPDATED]}) { node { id text } } } "
        ))

      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": [], "float": 1.23, "int": 1}}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "4",
          payload = s"""{"todo":{"node":{"id":"$testNodeId","text":"some todo"}}}"""
        )
      )
    }
  }

  "Create Subscription" should "support the node filters" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "3",
          query =
            "subscription asd($text: String!) { todo(where: {mutation_in: [CREATED] node: {text_contains: $text}}) { mutation node { id } previousValues { id text } updatedFields } }",
          variables = Json.obj("text" -> "some")
        )
      )

      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = s"""{"todo":{"mutation":"CREATED","node":{"id":"$testNodeId"},"previousValues":null,"updatedFields":null}}"""
        )
      )

      wsClient.sendMessage(stopMessage(id = "3"))
      wsClient.expectNoMessage(3.seconds)
    }
  }

  "Update Subscription" should "support the node filters" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(
          id = "3",
          query =
            "subscription asd($text: String!) { todo(where: {mutation_in: UPDATED AND: [{updatedFields_contains: \"text\"},{node: {text_contains: $text}}]}) { mutation previousValues { id json int } node { ...todo } } } fragment todo on Todo { id }",
          variables = Json.obj("text" -> "some")
        )
      )

      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": null, "int": 8, "createdAt": "2017"}}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = s"""{"todo":{"mutation":"UPDATED","previousValues":{"id":"text-node-id","json":null,"int":8},"node":{"id":"$testNodeId"}}}"""
        )
      )
    }
  }

  "Delete Subscription" should "ignore the node filters" in {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(id = "3",
                     query = "subscription { todo(where: {mutation_in: [DELETED]}) { node { ...todo } previousValues { id } } } fragment todo on Todo { id }")
      )

      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:deleteTodo"),
        s"""{"nodeId":"test-node-id2","node":{"id":"test-node-id2","text":"some text"},"modelId":"${model.name}","mutationType":"DeleteNode"}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "3",
          payload = """{"todo":{"node":null,"previousValues":{"id":"test-node-id2"}}}"""
        )
      )
    }
  }

  "Subscription" should "regenerate changed schema and work on reconnect" ignore {
    testInitializedWebsocket(project) { wsClient =>
      wsClient.sendMessage(
        startMessage(id = "create-filters", query = "subscription { todo(where:{node:{text_contains: \"important!\"}}) { node { id text } } }")
      )

      sleep(3000)

      invalidationTestKit.publish(Only(project.id), "")
      wsClient.expectMessage("""{"id":"create-filters","payload":{"message":"Schema changed"},"type":"error"}""")
      sleep()
      // KEEP WORKING ON RECONNECT

      wsClient.sendMessage(
        startMessage(id = "update-filters", query = "subscription { todo(where:{node:{text_contains: \"important!\"}}) { node { id text } } }")
      )

      sleep(3000)

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$importantTestNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": null, "createdAt": "2017"}}"""
      )

      wsClient.expectMessage(
        dataMessage(
          id = "update-filters",
          payload = s"""{"todo":{"node":{"id":"$importantTestNodeId","text":"important!"}}}"""
        )
      )

      wsClient.sendMessage(stopMessage("update-filters"))
    }
  }
}
