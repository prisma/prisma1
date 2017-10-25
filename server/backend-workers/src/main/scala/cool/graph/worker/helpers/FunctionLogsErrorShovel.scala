package cool.graph.worker.helpers

import java.util.concurrent.atomic.AtomicInteger

import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.messagebus.Conversions.ByteUnmarshaller
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.worker.payloads.{JsonConversions, LogItem}
import cool.graph.worker.utils.Utils
import org.joda.time.DateTime
import play.api.libs.json.{JsObject, Json}

import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

/**
  * Executable util to shovel messages out of the function logs error queue into the processing queue.
  * Restores routing key to normal 'mgs.0' and has fault-tolerant body parsing to transition failed messages to the
  * new error json format.
  */
object FunctionLogsErrorShovel extends App {
  import JsonConversions._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  case class OldLogItem(
      id: String,
      projectId: String,
      functionId: String,
      requestId: String,
      status: String,
      duration: Long,
      timestamp: String,
      message: String
  ) {
    def toLogItem: LogItem = {
      status match {
        case "SUCCESS" => LogItem(id, projectId, functionId, requestId, status, duration, timestamp, Json.parse(message).as[JsObject])
        case "FAILURE" => LogItem(id, projectId, functionId, requestId, status, duration, timestamp, Json.obj("error" -> message))
      }
    }
  }

  implicit val bugsnagger       = BugSnaggerImpl("")
  implicit val oldLogItemFormat = Json.format[OldLogItem]

  val amqpUri = sys.env("RABBITMQ_URI")

  val faultTolerantUnmarshaller: ByteUnmarshaller[LogItem] = { bytes =>
    Try { logItemUnmarshaller(bytes) }.orElse(fromOldLogItemFormat(bytes)) match {
      case Success(logItem) => logItem.copy(timestamp = correctLogTimestamp(logItem.timestamp))
      case Failure(err)     => throw err
    }
  }

  val marshaller      = JsonConversions.logItemMarshaller
  val targetPublisher = RabbitQueue.publisher[LogItem](amqpUri, "function-logs")
  val counter         = new AtomicInteger(0)

  val consumeFn = { msg: LogItem =>
    println(s"[FunctionLogsErrorShovel][${counter.incrementAndGet()}]] Re-processing: $msg")
    targetPublisher.publish(msg)
    Future.successful(())
  }

  val plainErrConsumer =
    RabbitQueue.plainConsumer[LogItem](amqpUri, "function-logs-error", "function-logs", autoDelete = false)(bugsnagger, faultTolerantUnmarshaller)

  def fromOldLogItemFormat(bytes: Array[Byte]): Try[LogItem] = Try { Json.parse(bytes).as[OldLogItem].toLogItem }

  def correctLogTimestamp(timestamp: String): String = {
    val dt     = DateTime.parse(timestamp)
    val newTst = Utils.msqlDateFormatter.print(dt)

    println(s"[FunctionLogsErrorShovel]\t$timestamp\t->\t$newTst")
    newTst
  }

  plainErrConsumer.withConsumer(consumeFn)

  println("Press enter to terminate...")
  scala.io.StdIn.readLine()
  println("Terminating.")

  plainErrConsumer.shutdown
  targetPublisher.shutdown
}
