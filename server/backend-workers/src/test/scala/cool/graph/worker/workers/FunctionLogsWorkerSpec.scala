package cool.graph.worker.workers

import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.messagebus.testkits.InMemoryQueueTestKit
import cool.graph.messagebus.testkits.spechelpers.InMemoryMessageBusTestKits
import cool.graph.worker.SpecHelper
import cool.graph.worker.payloads.LogItem
import org.joda.time.DateTime
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.Json
import slick.jdbc.MySQLProfile

import scala.concurrent.Await
import scala.util.{Failure, Success, Try}

class FunctionLogsWorkerSpec
    extends InMemoryMessageBusTestKits(SingleThreadedActorSystem("queueing-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {
  import slick.jdbc.MySQLProfile.api._

  import scala.concurrent.ExecutionContext.Implicits.global
  import scala.concurrent.duration._

  override def afterAll     = shutdownTestKit
  override def beforeEach() = SpecHelper.recreateLogsDatabase()

  def withLogsWorker(checkFn: (FunctionLogsWorker, InMemoryQueueTestKit[LogItem]) => Unit): Unit = {
    withQueueTestKit[LogItem] { testKit =>
      val logsDb                     = SpecHelper.getLogsDb
      val worker: FunctionLogsWorker = FunctionLogsWorker(logsDb, testKit)

      worker.start.futureValue

      def teardown = {
        testKit.shutdown()
        logsDb.close()
        worker.stop.futureValue
      }

      Try { checkFn(worker, testKit) } match {
        case Success(_) => teardown
        case Failure(e) => teardown; throw e
      }
    }
  }

  def getAllLogItemsCount(logsDb: MySQLProfile.api.Database) = Await.result(logsDb.run(sql"SELECT count(*) FROM Log".as[(Int)]), 2.seconds)

  "The FunctionLogsWorker" should {
    "work off valid items" in {
      withLogsWorker { (worker, testKit) =>
        val item1 = LogItem("id1", "pId1", "fId1", "reqId1", "SUCCESS", 123, DateTime.now.toLocalDateTime.toString(), Json.obj("test" -> "Testmessage1 😂"))
        val item2 = LogItem("id2", "pId2", "fId2", "reqId2", "FAILURE", 321, DateTime.now.toLocalDateTime.toString(), Json.obj("test" -> "Testmessage2 😂"))

        testKit.publish(item1)
        testKit.publish(item2)

        // Give the worker a bit of time to do the thing
        Thread.sleep(50)

        getAllLogItemsCount(worker.logsDb).head shouldBe 2
      }
    }
  }
}
