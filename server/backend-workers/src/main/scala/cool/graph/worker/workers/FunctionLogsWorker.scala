package cool.graph.worker.workers

import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.QueueConsumer
import cool.graph.worker.payloads.LogItem
import slick.jdbc.MySQLProfile.api._

import scala.concurrent.{ExecutionContext, Future}

case class FunctionLogsWorker(logsDb: Database, logsConsumer: QueueConsumer[LogItem])(implicit bugsnagger: BugSnagger, ec: ExecutionContext) extends Worker {
  lazy val consumerRef = logsConsumer.withConsumer(consumeFn)

  private val consumeFn = (i: LogItem) => {
    val reqCuid = i.requestId.split(":").lastOption.getOrElse(i.requestId)

    logsDb.run(sqlu"""
      INSERT INTO Log (id, projectId, functionId, requestId, status, duration, timestamp, message)
      VALUES(${i.id}, ${i.projectId}, ${i.functionId}, $reqCuid, ${i.status}, ${i.duration}, ${i.timestamp}, ${i.message.toString()})
    """)
  }

  override def start: Future[_] = Future { consumerRef }
  override def stop: Future[_]  = Future { consumerRef.stop }
}
