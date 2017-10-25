package cool.graph.worker.workers
import cool.graph.bugsnag.BugSnagger
import cool.graph.cuid.Cuid
import cool.graph.messagebus.{QueueConsumer, QueuePublisher}
import cool.graph.utils.future.FutureUtils._
import cool.graph.worker.payloads.{LogItem, Webhook}
import cool.graph.worker.utils.Utils
import play.api.libs.json.{JsArray, JsObject, Json}
import play.api.libs.ws.ahc.StandaloneAhcWSClient

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

case class WebhookDelivererWorker(
    httpClient: StandaloneAhcWSClient,
    webhooksConsumer: QueueConsumer[Webhook],
    logsPublisher: QueuePublisher[LogItem]
)(implicit bugsnagger: BugSnagger, ec: ExecutionContext)
    extends Worker {
  import scala.concurrent.ExecutionContext.Implicits.global

  // Current decision: Do not retry delivery, treat all return codes as work item "success" (== ack).
  val consumeFn = (wh: Webhook) => {
    val req       = httpClient.url(wh.url).withHttpHeaders(wh.headers.toList :+ ("Content-Type", "application/json"): _*).withRequestTimeout(5.seconds)
    val startTime = System.currentTimeMillis()
    val response  = req.post(wh.payload)

    response.toFutureTry.flatMap {
      case Success(resp) =>
        val timing    = System.currentTimeMillis() - startTime
        val body      = resp.body[String]
        val timestamp = Utils.msqlDateTime3Timestamp()

        val logItem = resp.status match {
          case x if x >= 200 && x < 300 =>
            val functionReturnValue = formatFunctionSuccessMessage(wh.payload, body)
            LogItem(Cuid.createCuid(), wh.projectId, wh.functionId, wh.requestId, "SUCCESS", timing, timestamp, functionReturnValue)

          case x =>
            val message = s"Call to ${wh.url} failed with status $x, response body $body and headers [${formatHeaders(resp.headers)}]"
            LogItem(Cuid.createCuid(), wh.projectId, wh.functionId, wh.requestId, "FAILURE", timing, timestamp, formatFunctionErrorMessage(message))
        }

        logsPublisher.publish(logItem)
        Future.successful(())

      case Failure(err) =>
        val timing    = System.currentTimeMillis() - startTime
        val message   = s"Call to ${wh.url} failed with: ${err.getMessage}"
        val timestamp = Utils.msqlDateTime3Timestamp()
        val logItem   = LogItem(Cuid.createCuid(), wh.projectId, wh.functionId, wh.requestId, "FAILURE", timing, timestamp, formatFunctionErrorMessage(message))

        logsPublisher.publish(logItem)
        Future.successful(())
    }
  }

  lazy val consumerRef = webhooksConsumer.withConsumer(consumeFn)

  /**
    * Formats a given map of headers to a single line string representation "H1: V1 | H2: V2 ...".
    *
    * @param headers The headers to format
    * @return A single-line string in the format "header: value | nextHeader: value ...".
    *         If multiple values per header are given, they are treated as separate instances of the same header.
    *         E.g. X-Test-Header: 1 | X-Test-Header: 2 | Content-Type: appliation/json
    */
  def formatHeaders(headers: Map[String, Seq[String]]): String = {
    headers.flatMap(header => header._2.map(headerValue => s"${header._1}: $headerValue")).mkString(" | ")
  }

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
