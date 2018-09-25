package com.prisma.subscriptions.specs

import com.prisma.shared.schema_dsl.SchemaDsl
import org.scalatest.{FlatSpec, Matchers}
import pdi.jwt.{Jwt, JwtAlgorithm}

class SubscriptionsAuthSpec extends FlatSpec with Matchers with SubscriptionSpecBase {

  "the subscriptions" should "succeed without an auth token if the project has no secrets" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field("text", _.String)
    }
    project.secrets should be(empty)

    testWebsocketV07(project) { wsClient =>
      wsClient.sendMessage(connectionInit)
      wsClient.expectMessage(connectionAck)
    }
  }

  "the subscriptions" should "succeed with an arbitrary token if the project has no secrets" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field("text", _.String)
    }
    project.secrets should be(empty)

    testWebsocketV07(project) { wsClient =>
      wsClient.sendMessage(connectionInit("arbitrary token"))
      wsClient.expectMessage(connectionAck)
    }
  }

  "the subscriptions" should "succeed if the provided token is valid for a project with a secret" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field("text", _.String)
    }
    val actualProject = project.copy(secrets = Vector("other_secret", "secret"))
    val validToken    = Jwt.encode("{}", "secret", JwtAlgorithm.HS256)

    testWebsocketV07(actualProject) { wsClient =>
      wsClient.sendMessage(connectionInit(validToken))
      wsClient.expectMessage(connectionAck)
    }
  }

  "the subscriptions" should "fail if no token is provided for a project with a secret" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field("text", _.String)
    }
    val actualProject = project.copy(secrets = Vector("secret"))

    testWebsocketV07(actualProject) { wsClient =>
      wsClient.sendMessage(connectionInit)
      wsClient.expectMessage(s"""{"payload":{"message":"Authentication token is invalid."},"type":"connection_error"}""")
    }
  }

  "the subscriptions" should "fail if the provided token is invalid for a project with a secret" in {
    val project = SchemaDsl.fromBuilder { schema =>
      schema.model("Todo").field("text", _.String)
    }
    val actualProject = project.copy(secrets = Vector("secret"))
    val invalidToken  = Jwt.encode("{}", "other-secret", JwtAlgorithm.HS256)

    testWebsocketV07(actualProject) { wsClient =>
      wsClient.sendMessage(connectionInit(invalidToken))
      wsClient.expectMessage(s"""{"payload":{"message":"Authentication token is invalid."},"type":"connection_error"}""")
    }
  }

}
