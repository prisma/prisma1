package com.prisma.singleserver

import com.prisma.subscriptions.{Webhook => ApiWebhook}
import com.prisma.messagebus.Conversions.Converter
import com.prisma.subscriptions.protocol.SubscriptionRequest
import com.prisma.websocket.protocol.Request
import com.prisma.workers.payloads.{Webhook => WorkerWebhook}

object Converters {
  val websocketRequest2SubscriptionRequest = { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }

  val apiWebhook2WorkerWebhook: Converter[ApiWebhook, WorkerWebhook] = { wh: ApiWebhook =>
    WorkerWebhook(wh.projectId, wh.functionName, wh.requestId, wh.url, wh.payload, wh.id, wh.headers)
  }
}
