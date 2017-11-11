package cool.graph.singleserver

import cool.graph.messagebus.Conversions.Converter
import cool.graph.subscriptions.protocol.SubscriptionRequest
import cool.graph.webhook.Webhook
import cool.graph.websockets.protocol.Request
import cool.graph.worker.payloads.{LogItem, Webhook => WorkerWebhook}
import play.api.libs.json.{JsError, JsSuccess, Json}

/**
  * Necessary converters to make queueing and pubsub possible inmemory.
  */
object Converters {

  import cool.graph.worker.payloads.JsonConversions.logItemFormat

  val apiWebhook2WorkerWebhook: Converter[Webhook, WorkerWebhook] = { wh: Webhook =>
    WorkerWebhook(wh.projectId, wh.functionId, wh.requestId, wh.url, wh.payload, wh.id, wh.headers)
  }

  val string2LogItem = { str: String =>
    Json.parse(str).validate[LogItem] match {
      case JsSuccess(logItem, _) => logItem
      case JsError(e)            => sys.error(s"Invalid log item $str, ignoring message.")
    }
  }

  val websocketRequest2SubscriptionRequest = { req: Request =>
    SubscriptionRequest(req.sessionId, req.projectId, req.body)
  }
}
