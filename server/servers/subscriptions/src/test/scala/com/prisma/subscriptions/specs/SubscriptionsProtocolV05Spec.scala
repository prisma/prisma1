package com.prisma.subscriptions.specs

import com.prisma.messagebus.pubsub.Only
import com.prisma.shared.models.Model
import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest._
import play.api.libs.json._

import scala.concurrent.duration._

class SubscriptionsProtocolV05Spec extends FlatSpec with Matchers with SubscriptionSpecBase {

  val project = SchemaDsl.fromBuilder { schema =>
    val todo = schema
      .model("Todo")
      .field("text", _.String)
      .field("json", _.Json)
      .field("int", _.Int)

  }

  val model: Model = project.schema.getModelByName_!("Todo")

  var testNodeId, importantTestNodeId: String = null

  override def beforeEach() = {
    super.beforeEach()
    testDatabase.setup(project)
    val json = Json.arr(1, 2, Json.obj("a" -> "b"))
    testNodeId = TestData.createTodo("some todo", json, None, project, model, testDatabase).value.toString
    importantTestNodeId = TestData.createTodo("important!", json, None, project, model, testDatabase).value.toString
  }

  "All subscriptions" should "support the basic subscriptions protocol when id is string" in {
    testWebsocketV05(project) { wsClient =>
      wsClient.sendMessage("{}")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage("")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage(s"""{"type":"init","payload":{}}""")
      wsClient.expectMessage("""{"type":"init_success"}""")

      // CREATE
      wsClient.sendMessage("""{"type":"subscription_start","id":"ioPRfgqN6XMefVW6","variables":{},"query":"subscription { createTodo { id text json } }"}""")
      wsClient.expectMessage(
        """{"id":"ioPRfgqN6XMefVW6","payload":{"errors":[{"message":"The provided query doesn't include any known model name. Please check for the latest subscriptions API."}]},"type":"subscription_fail"}"""
      )

      wsClient.sendMessage("""{"type":"subscription_start","id":"ioPRfgqN6XMefVW6","variables":{},"query":"subscription { todo { node { id text json } } }"}""")
      wsClient.expectMessage("""{"id":"ioPRfgqN6XMefVW6","type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        s"""{"id":"ioPRfgqN6XMefVW6","payload":{"data":{"todo":{"node":{"id":"$testNodeId","text":"some todo","json":[1,2,{"a":"b"}]}}}},"type":"subscription_data"}""")

      wsClient.sendMessage("""{"type":"subscription_end","id":"ioPRfgqN6XMefVW6"}""")

      // should work with operationName
      wsClient.sendMessage(
        """{"type":"subscription_start","id":"2","variables":null,"query":"subscription x { todo(where: {mutation_in: [CREATED]}) { node { id } } }  mutation y { createTodo { id } }","operationName":"x"}""")
      wsClient.expectMessage("""{"id":"2","type":"subscription_success"}""")

      // should work without variables
      wsClient.sendMessage(
        """{"type":"subscription_start","id":"3","query":"subscription x { todo(where: {mutation_in: [CREATED]}) { node { id } } }  mutation y { createTodo { id } }","operationName":"x"}""")
      wsClient.expectMessage("""{"id":"3","type":"subscription_success"}""")

      // DELETE
      wsClient.sendMessage(
        """{"type":"subscription_start","id":"4","query":"subscription x { todo(where: {mutation_in: [DELETED]}) { node { id } } }  mutation y { createTodo { id } }","operationName":"x"}""")
      wsClient.expectMessage("""{"id":"4","type":"subscription_success"}""")
      sleep()
      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:deleteTodo"),
        s"""{"nodeId":"$testNodeId","node":{"id":"$testNodeId","text":"some text"},"modelId":"${model.name}","mutationType":"DeleteNode"}"""
      )

      wsClient.expectMessage("""{"id":"4","payload":{"data":{"todo":{"node":null}}},"type":"subscription_data"}""")

      // UPDATE
      wsClient.sendMessage(
        """{"type":"subscription_start","id":"5","variables":{},"query":"subscription { todo(where: {mutation_in: [UPDATED]}) { node { id text } } } "}""")
      wsClient.expectMessage("""{"id":"5","type":"subscription_success"}""")

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": []}}"""
      )

      wsClient.expectMessage(s"""{"id":"5","payload":{"data":{"todo":{"node":{"id":"$testNodeId","text":"some todo"}}}},"type":"subscription_data"}""")

    }
  }

  "All subscriptions" should "support the basic subscriptions protocol when id is number, part 1" in {
    testWebsocketV05(project) { wsClient =>
      wsClient.sendMessage("{}")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage("")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage(s"""{"type":"init","payload":{}}""")
      wsClient.expectMessage("""{"type":"init_success"}""")

      // CREATE
      wsClient.sendMessage("""{"type":"subscription_start","id":1,"variables":{},"query":"subscription { createTodo { id text json } }"}""")
      wsClient.expectMessage(
        """{"id":1,"payload":{"errors":[{"message":"The provided query doesn't include any known model name. Please check for the latest subscriptions API."}]},"type":"subscription_fail"}"""
      )

      wsClient.sendMessage("""{"type":"subscription_start","id":1,"variables":{},"query":"subscription { todo { node { id text json } } }"}""")
      wsClient.expectMessage("""{"id":1,"type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        s"""{"id":1,"payload":{"data":{"todo":{"node":{"id":"$testNodeId","text":"some todo","json":[1,2,{"a":"b"}]}}}},"type":"subscription_data"}""")

      wsClient.sendMessage("""{"type":"subscription_end","id":1}""")

      // should work with operationName
      wsClient.sendMessage(
        """{"type":"subscription_start","id":2,"variables":null,"query":"subscription x { todo(where: {mutation_in: [CREATED]}) { node { id } } }  mutation y { createTodo { id } }","operationName":"x"}""")
      wsClient.expectMessage("""{"id":2,"type":"subscription_success"}""")

      // should work without variables
      wsClient.sendMessage(
        """{"type":"subscription_start","id":3,"query":"subscription x { todo(where: {mutation_in: [CREATED]}) { node { id } } }  mutation y { createTodo { id } }","operationName":"x"}""")
      wsClient.expectMessage("""{"id":3,"type":"subscription_success"}""")

      // DELETE
      wsClient.sendMessage(
        """{"type":"subscription_start","id":4,"query":"subscription x { todo(where: {mutation_in: [DELETED]}) { node { id } } }  mutation y { createTodo { id } }","operationName":"x"}""")
      wsClient.expectMessage("""{"id":4,"type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:deleteTodo"),
        s"""{"nodeId":"$testNodeId","node":{"id":"$testNodeId","text":"some text"},"modelId":"${model.name}","mutationType":"DeleteNode"}"""
      )

//      sleep(500)
      wsClient.expectMessage("""{"id":4,"payload":{"data":{"todo":{"node":null}}},"type":"subscription_data"}""")

      // UPDATE
      wsClient.sendMessage(
        """{"type":"subscription_start","id":5,"variables":{},"query":"subscription { todo(where: {mutation_in: [UPDATED]}) { node { id text } } } "}""")
      wsClient.expectMessage("""{"id":5,"type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": []}}"""
      )

//      sleep(500)
      wsClient.expectMessage(s"""{"id":5,"payload":{"data":{"todo":{"node":{"id":"$testNodeId","text":"some todo"}}}},"type":"subscription_data"}""")

    }
  }

  "Create Subscription" should "support the node filters" in {
    testWebsocketV05(project) { wsClient =>
      // CREATE
      // should work with variables
      wsClient.sendMessage("{}")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage(s"""{"type":"init","payload":{}}""")
      wsClient.expectMessage("""{"type":"init_success"}""")

      wsClient.sendMessage(
        """{
            "type":"subscription_start",
            "id":"3",
            "query":"subscription asd($text: String!) { todo(where: {mutation_in: [CREATED] node: {text_contains: $text}}) { mutation node { id } previousValues { id text } updatedFields } }",
            "variables": {"text": "some"}
            }""".stripMargin)
      wsClient.expectMessage("""{"id":"3","type":"subscription_success"}""")

      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:createTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"CreateNode"}"""
      )

      wsClient.expectMessage(
        s"""{"id":"3","payload":{"data":{"todo":{"mutation":"CREATED","node":{"id":"$testNodeId"},"previousValues":null,"updatedFields":null}}},"type":"subscription_data"}""")

      wsClient.sendMessage("""{"type":"subscription_end"}""")
      wsClient.expectNoMessage(3.seconds)
    }
  }

  "Update Subscription" should "support the node filters" in {
    testWebsocketV05(project) { wsClient =>
      // CREATE
      // should work with variables
      wsClient.sendMessage("{}")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage(s"""{"type":"init","payload":{}}""")
      wsClient.expectMessage("""{"type":"init_success"}""")

      wsClient.sendMessage(
        """{
            "type":"subscription_start",
            "id":"3",
            "query":"subscription asd($text: String!) { todo(where: {mutation_in: UPDATED AND: [{updatedFields_contains: \"text\"},{node: {text_contains: $text}}]}) { mutation previousValues { id json int } node { ...todo } } } fragment todo on Todo { id }",
            "variables": {"text": "some"}
            }""".stripMargin)
      wsClient.expectMessage("""{"id":"3","type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$testNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": null, "int": 8, "createdAt": "2017"}}"""
      )

      wsClient.expectMessage(
        s"""{"id":"3","payload":{"data":{"todo":{"mutation":"UPDATED","previousValues":{"id":"text-node-id","json":null,"int":8},"node":{"id":"$testNodeId"}}}},"type":"subscription_data"}""")
    }
  }

  "Delete Subscription" should "ignore the node filters" in {
    testWebsocketV05(project) { wsClient =>
      // should work with variables
      wsClient.sendMessage("{}")
      wsClient.expectMessage(cantBeParsedError)

      wsClient.sendMessage(s"""{"type":"init","payload":{}}""")
      wsClient.expectMessage("""{"type":"init_success"}""")

      wsClient.sendMessage(
        """{
            "type":"subscription_start",
            "id":"3",
            "query":"subscription { todo(where: {mutation_in: [DELETED]}) { node { ...todo } previousValues { id } } } fragment todo on Todo { id }"
            }""".stripMargin)
      wsClient.expectMessage("""{"id":"3","type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:deleteTodo"),
        s"""{"nodeId":"test-node-id2","node":{"id":"test-node-id2","text":"some text"},"modelId":"${model.name}","mutationType":"DeleteNode"}"""
      )

      wsClient.expectMessage("""{"id":"3","payload":{"data":{"todo":{"node":null,"previousValues":{"id":"test-node-id2"}}}},"type":"subscription_data"}""")
    }
  }

  "Subscription" should "regenerate changed schema and work on reconnect" ignore {
    testWebsocketV05(project) { wsClient =>
      // SCHEMA INVALIDATION

      wsClient.sendMessage(s"""{"type":"init","payload":{}}""")
      wsClient.expectMessage("""{"type":"init_success"}""")

      wsClient.sendMessage(
        """{"type":"subscription_start","id":"create-filters","variables":{},"query":"subscription { todo(where:{node:{text_contains: \"important!\"}}) { node { id text } } }"}""")
      wsClient.expectMessage("""{"id":"create-filters","type":"subscription_success"}""")
      sleep()

      invalidationTestKit.publish(Only(project.id), "")
      wsClient.expectMessage("""{"id":"create-filters","payload":{"errors":[{"message":"Schema changed"}]},"type":"subscription_fail"}""")
      sleep()

      // KEEP WORKING ON RECONNECT

      wsClient.sendMessage(
        """{"type":"subscription_start","id":"update-filters","variables":{},"query":"subscription { todo(where:{node:{text_contains: \"important!\"}}) { node { id text } } }"}""")
      wsClient.expectMessage("""{"id":"update-filters","type":"subscription_success"}""")
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:updateTodo"),
        s"""{"nodeId":"$importantTestNodeId","modelId":"${model.name}","mutationType":"UpdateNode","changedFields":["text"], "previousValues": {"id": "text-node-id", "text": "asd", "json": null, "createdAt": "2017"}}"""
      )

      wsClient.expectMessage(
        s"""{"id":"update-filters","payload":{"data":{"todo":{"node":{"id":"$importantTestNodeId","text":"important!"}}}},"type":"subscription_data"}""")

      wsClient.sendMessage("""{"type":"subscription_end","id":"update-filters"}""")
    }
  }

  override def failTest(msg: String): Nothing = { // required by RouteTest
    throw new Error("Test failed: " + msg)
  }
}
