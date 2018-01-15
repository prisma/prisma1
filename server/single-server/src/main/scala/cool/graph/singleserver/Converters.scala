package cool.graph.singleserver

import cool.graph.subscriptions.{Webhook => ApiWebhook}
import cool.graph.messagebus.Conversions.Converter
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.websocket.protocol.Request
import cool.graph.workers.payloads.{Webhook => WorkerWebhook}

object Converters {
  val websocketRequest2SubscriptionRequest = { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }

  val apiWebhook2WorkerWebhook: Converter[ApiWebhook, WorkerWebhook] = { wh: ApiWebhook =>
    WorkerWebhook(wh.projectId, wh.functionName, wh.requestId, wh.url, wh.payload, wh.id, wh.headers)
  }
}
