package cool.graph.subscriptions.specs

import cool.graph.messagebus.pubsub.Only
import cool.graph.shared.project_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import pdi.jwt.{Jwt, JwtAlgorithm}

class SubscriptionsAuthSpec extends FlatSpec with Matchers with SpecBase {
  import cool.graph.subscriptions.specs.WSProbeExtensions._

  "the subscriptions" should "work without an auth header if the project has no secrets" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field("text", _.String)
    }
    val model = project.schema.getModelByName_!("Todo")

    testInitializedWebsocket(project) { wsClient =>
      val id = "ioPRfgqN6XMefVW6"
      // create
      wsClient.sendMessage(
        startMessage(
          id = id,
          query = """
                    | subscription {
                    |   todo(where: {mutation_in: DELETED}) {
                    |     previousValues {
                    |       text
                    |     }
                    |   }
                    | }
                    | """.stripMargin
        )
      )
      sleep()

      sssEventsTestKit.publish(
        Only(s"subscription:event:${project.id}:deleteTodo"),
        s"""{"nodeId":"test-node-id","modelId":"${model.id}","mutationType":"DeleteNode"}"""
      )

      wsClient.expectMessageContains(
        s"""{"id":"$id","payload":{"data":{"Todo":{"node":{"text":"some todo","done":null}}},"errors":[{"locations":[{"line":6,"column":8}],"path":["Todo","node","done"],"code":3008,"message":"Insufficient Permissions"""")

      wsClient.sendMessage(stopMessage(id))
    }
  }

  "the subscriptions" should "fail if the provided token is invalid for a project with a secret" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field("text", _.String)
    }
    val actualProject = project.copy(secrets = Vector("secret"))

    testWebsocket(actualProject) { wsClient =>
      wsClient.sendMessage(connectionInit("invalid token"))
      wsClient.expectMessage(connectionError)
    }
  }

  "the subscriptions" should "succeed if the provided token is invalid for a project with a secret" in {
    val project = SchemaDsl() { schema =>
      schema.model("Todo").field("text", _.String)
    }
    val actualProject = project.copy(secrets = Vector("other_secret", "secret"))
    val token         = Jwt.encode("{}", "secret", JwtAlgorithm.HS256)

    testWebsocket(actualProject) { wsClient =>
      wsClient.sendMessage(connectionInit(token))
      wsClient.expectMessage(connectionAck)
    }
  }

}
