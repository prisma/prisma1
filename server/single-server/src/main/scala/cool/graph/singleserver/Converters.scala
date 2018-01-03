package cool.graph.singleserver

import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.websocket.protocol.Request

object Converters {
  val websocketRequest2SubscriptionRequest = { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }
}
