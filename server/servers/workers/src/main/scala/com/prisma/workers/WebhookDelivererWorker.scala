package com.prisma.workers

import akka.http.scaladsl.model.ContentTypes
import com.prisma.akkautil.http.{RequestFailedError, SimpleHttpClient}
import com.prisma.messagebus.QueueConsumer
import com.prisma.workers.payloads.Webhook
import play.api.libs.json.{JsArray, JsObject, Json}

import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class WebhookDelivererWorker(
    httpClient: SimpleHttpClient,
    webhooksConsumer: QueueConsumer[Webhook]
    //logsPublisher: QueuePublisher[LogItem]
)(implicit ec: ExecutionContext)
    extends Worker {

  // Current decision: Do not retry delivery, treat all return codes as work item "success" (== ack).
  val consumeFn = (wh: Webhook) => {
//    val startTime = System.currentTimeMillis()
//    def handleError(msg: String) = {
//      val timing    = System.currentTimeMillis() - startTime
//      val timestamp = Utils.msqlDateTime3Timestamp()
////      val logItem   = LogItem(Cuid.createCuid(), wh.projectId, wh.functionId, wh.requestId, "FAILURE", timing, timestamp, formatFunctionErrorMessage(msg))
//
//      logsPublisher.publish(logItem)
//    }

    httpClient
      .post(wh.url, wh.payload, ContentTypes.`application/json`, wh.headers.toList)
      .recover {
        case e: RequestFailedError =>
          val message =
            s"Call to ${wh.url} failed with status ${e.response.status}, response body '${e.response.body.getOrElse("")}' and headers [${formatHeaders(e.response.headers)}]"
          println(message)

        case e: Throwable =>
          val message = s"Call to ${wh.url} failed with: ${e.getMessage}"
          println(message)
      }
  }

  lazy val consumerRef = webhooksConsumer.withConsumer(consumeFn)

  /**
    * Formats a given map of headers to a single line string representation "H1: V1 | H2: V2 ...".
    *
    * @param headers The headers to format
    * @return A single-line string in the format "header: value | nextHeader: value ...".
    */
  def formatHeaders(headers: Seq[(String, String)]): String = headers.map(header => s"${header._1}: ${header._2}").mkString(" | ")

  /**
    * Formats a function log message according to our schema.
    *
    * @param payload Payload send with the webhook delivery.
    * @param responseBody Webhook delivery return body
    * @return A JsObject that can be used in the log message field of the function log.
    */
  def formatFunctionSuccessMessage(payload: String, responseBody: String): JsObject = {
    val returnValue = Try { Json.parse(responseBody).validate[JsObject].get } match {
      case Success(json) => json
      case Failure(_)    => Json.obj("rawResponse" -> responseBody)
    }

    Json.obj(
      "event"       -> payload,
      "logs"        -> (returnValue \ "logs").getOrElse(JsArray(Seq.empty)),
      "returnValue" -> returnValue
    )
  }

  /**
    * Formats a function log error message according to our schema.
    *
    * @param errMsg Payload send with the webhook delivery.
    * @return A JsObject that can be used in the log message field of the function log.
    */
  def formatFunctionErrorMessage(errMsg: String): JsObject = Json.obj("error" -> errMsg)

  override def start: Future[_] = Future { consumerRef }
  override def stop: Future[_]  = Future { consumerRef.stop }
}
