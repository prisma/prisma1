package cool.graph.worker.workers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.messagebus.testkits.RabbitQueueTestKit
import cool.graph.worker.payloads.{JsonConversions, LogItem}
import cool.graph.worker.services.{WorkerCloudServices, WorkerServices}
import cool.graph.worker.SpecHelper
import cool.graph.worker.utils.Env
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.Json

import scala.concurrent.Await
import scala.util.Try

class FunctionLogsWorkerSpec
    extends TestKit(ActorSystem("queueing-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures {
  import slick.jdbc.MySQLProfile.api._
  import JsonConversions._
  import scala.concurrent.duration._
  import scala.concurrent.ExecutionContext.Implicits.global

  var services: WorkerServices                      = _
  var logItemPublisher: RabbitQueueTestKit[LogItem] = _
  var worker: FunctionLogsWorker                    = _

  implicit val materializer = ActorMaterializer()
  implicit val bugSnagger   = BugSnaggerImpl("")

  override def beforeEach(): Unit = {
    SpecHelper.recreateLogsDatabase()

    services = WorkerCloudServices()
    worker = FunctionLogsWorker(services.logsDb, services.logsQueue)
    worker.start.futureValue

    logItemPublisher = RabbitQueueTestKit[LogItem](Env.clusterLocalRabbitUri, "function-logs")
  }

  override def afterEach(): Unit = {
    services.shutdown

    Try { worker.stop.futureValue }
    Try { logItemPublisher.shutdown() }
  }

  override def afterAll = shutdown(verifySystemShutdown = true)

  def getAllLogItemsCount() = Await.result(services.logsDb.run(sql"SELECT count(*) FROM Log".as[(Int)]), 2.seconds)

  "The FunctionLogsWorker" should {
    "work off valid items" in {
      val item1 = LogItem("id1", "pId1", "fId1", "reqId1", "SUCCESS", 123, DateTime.now.toLocalDateTime.toString(), Json.obj("test" -> "Testmessage1 😂"))
      val item2 =
        s"""
          {
            "id": "id2",
            "projectId": "pId2",
            "functionId": "fId2",
            "requestId": "reqId2",
            "status": "FAILURE",
            "duration": 321,
            "timestamp": "${DateTime.now.toLocalDateTime.toString()}",
            "message": {
              "test": "Testmessage2 😂😂😂"
            }
          }
        """.stripMargin

      logItemPublisher.publish(item1)
      logItemPublisher.publishPlain("msg.0", item2)

      // Give the worker a bit of time to do the thing
      Thread.sleep(2000)

      getAllLogItemsCount().head shouldBe 2
    }

    "ignore invalid items and work off valid ones regardless" in {
      val item1 = LogItem("id1", "pId1", "fId1", "reqId1", "SUCCESS", 123, DateTime.now.toLocalDateTime.toString(), Json.obj("test" -> "Testmessage 😂"))

      // Publish an invalid log item
      logItemPublisher.publishPlain("msg.0", "Invalid (should be a full json)")
      logItemPublisher.publish(item1)

      // Give the worker a bit of time to do the thing. Why is the latency so gigantic?
      Thread.sleep(10000)

      getAllLogItemsCount().head shouldBe 1
    }
  }
}
