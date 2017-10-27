package cool.graph.worker.services

import akka.stream.ActorMaterializer
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.queue.LinearBackoff
import cool.graph.messagebus.queue.rabbit.RabbitQueue
import cool.graph.messagebus.{Queue, QueueConsumer}
import cool.graph.worker.payloads.{LogItem, Webhook}
import cool.graph.worker.utils.Env
import play.api.libs.ws.ahc.StandaloneAhcWSClient
import slick.jdbc.MySQLProfile

import scala.concurrent.duration._

trait WorkerServices {
  val logsDb: MySQLProfile.backend.Database
  val httpClient: StandaloneAhcWSClient
  val logsQueue: Queue[LogItem]
  val webhooksConsumer: QueueConsumer[Webhook]

  def shutdown: Unit
}

case class WorkerCloudServices()(implicit materializer: ActorMaterializer, bugsnagger: BugSnagger) extends WorkerServices {
  import cool.graph.worker.payloads.JsonConversions._

  lazy val httpClient = StandaloneAhcWSClient()

  lazy val logsDb: MySQLProfile.backend.Database = {
    import slick.jdbc.MySQLProfile.api._
    Database.forConfig("logs")
  }

  lazy val webhooksConsumer: QueueConsumer[Webhook] = RabbitQueue.consumer[Webhook](Env.clusterLocalRabbitUri, "webhooks")
  lazy val logsQueue: RabbitQueue[LogItem]          = RabbitQueue[LogItem](Env.clusterLocalRabbitUri, "function-logs", LinearBackoff(5.seconds))

  def shutdown: Unit = {
    httpClient.close()
    logsDb.close()
    logsQueue.shutdown
    webhooksConsumer.shutdown
  }
}

// In the dev version the queueing impls are created / injected above the services.
case class WorkerDevServices(webhooksConsumer: QueueConsumer[Webhook], logsQueue: Queue[LogItem])(implicit materializer: ActorMaterializer)
    extends WorkerServices {
  lazy val httpClient = StandaloneAhcWSClient()

  lazy val logsDb: MySQLProfile.backend.Database = {
    import slick.jdbc.MySQLProfile.api._
    Database.forConfig("logs")
  }

  def shutdown: Unit = {
    httpClient.close()
    logsDb.close()
    logsQueue.shutdown
    webhooksConsumer.shutdown
  }
}
