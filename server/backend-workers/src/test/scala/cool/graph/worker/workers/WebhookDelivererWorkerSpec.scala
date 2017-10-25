package cool.graph.worker.workers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import cool.graph.bugsnag.BugSnaggerImpl
import cool.graph.messagebus.testkits.RabbitQueueTestKit
import cool.graph.stub.Import.withStubServer
import cool.graph.stub.StubDsl.Default.Request
import cool.graph.worker.payloads.{JsonConversions, LogItem, Webhook}
import cool.graph.worker.services.{WorkerCloudServices, WorkerServices}
import cool.graph.worker.SpecHelper
import cool.graph.worker.utils.Env
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.{JsObject, Json}

import scala.util.Try
import scala.concurrent.duration._

class WebhookDelivererWorkerSpec
    extends TestKit(ActorSystem("webhookSpec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures {
  import JsonConversions._
  import scala.concurrent.ExecutionContext.Implicits.global

  var services: WorkerServices                      = _
  var worker: WebhookDelivererWorker                = _
  var webhookPublisher: RabbitQueueTestKit[Webhook] = _
  var logsConsumer: RabbitQueueTestKit[LogItem]     = _

  implicit val materializer = ActorMaterializer()
  implicit val bugSnagger   = BugSnaggerImpl("")

  override def beforeEach(): Unit = {
    SpecHelper.recreateLogsDatabase()

    services = WorkerCloudServices()
    worker = WebhookDelivererWorker(services.httpClient, services.webhooksConsumer, services.logsQueue)
    worker.start.futureValue

    webhookPublisher = RabbitQueueTestKit[Webhook](Env.clusterLocalRabbitUri, "webhooks")
    logsConsumer = RabbitQueueTestKit[LogItem](Env.clusterLocalRabbitUri, "function-logs")

    logsConsumer.withTestConsumers()
  }

  override def afterEach(): Unit = {
    services.shutdown

    Try { worker.stop.futureValue }
    Try { webhookPublisher.shutdown() }
    Try { logsConsumer.shutdown() }
  }

  override def afterAll = shutdown(verifySystemShutdown = true)

  "The webhooks delivery worker" should {
    "work off items and log a success message if the delivery was successful" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(200, """{"data": "stuff", "logs": ["log1", "log2"]}""")
          .ignoreBody)

      withStubServer(stubs).withArg { server =>
        val webhook =
          Webhook(
            "pid",
            "fid",
            "rid",
            s"http://localhost:${server.port}/function-endpoint",
            "GIGAPIZZA",
            "someId",
            Map("X-Cheese-Header" -> "Gouda")
          )

        webhookPublisher.publish(webhook)

        // Give the worker time to work off
        Thread.sleep(1000)

        logsConsumer.expectMsgCount(1)

        val logMessage: LogItem = logsConsumer.messages.head

        logMessage.projectId shouldBe "pid"
        logMessage.functionId shouldBe "fid"
        logMessage.requestId shouldBe "rid"
        logMessage.id shouldNot be(empty)
        logMessage.status shouldBe "SUCCESS"
        logMessage.timestamp shouldNot be(empty)
        logMessage.duration > 0 shouldBe true
        logMessage.message shouldBe a[JsObject]
        (logMessage.message \ "event").get.as[String] shouldBe "GIGAPIZZA"
        (logMessage.message \ "logs").get.as[Seq[String]] shouldBe Seq("log1", "log2")
        (logMessage.message \ "returnValue").get shouldBe Json.obj("data" -> "stuff", "logs" -> Seq("log1", "log2"))
      }
    }

    "work off items and log a failure message if the delivery was unsuccessful" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(400, """{"error": what are you doing?"}""")
          .ignoreBody)

      withStubServer(stubs).withArg { server =>
        val webhook =
          Webhook(
            "pid",
            "fid",
            "rid",
            s"http://localhost:${server.port}/function-endpoint",
            "GIGAPIZZA",
            "someId",
            Map("X-Cheese-Header" -> "Gouda")
          )

        webhookPublisher.publish(webhook)
        logsConsumer.expectMsgCount(1)

        val logMessage: LogItem = logsConsumer.messages.head

        logMessage.projectId shouldBe "pid"
        logMessage.functionId shouldBe "fid"
        logMessage.requestId shouldBe "rid"
        logMessage.id shouldNot be(empty)
        logMessage.status shouldBe "FAILURE"
        logMessage.timestamp shouldNot be(empty)
        logMessage.duration > 0 shouldBe true
        logMessage.message shouldBe a[JsObject]
        (logMessage.message \ "error").get.as[String] should include("what are you doing?")
      }
    }

    "work off items and log a failure message if the delivery was unsuccessful due to the http call itself failing (e.g. timeout or not available)" in {
      val webhook =
        Webhook(
          "pid",
          "fid",
          "rid",
          s"http://thishosthopefullydoesntexist123/function-endpoint",
          "GIGAPIZZA",
          "someId",
          Map("X-Cheese-Header" -> "Gouda")
        )

      webhookPublisher.publish(webhook)
      logsConsumer.expectMsgCount(1)

      val logMessage: LogItem = logsConsumer.messages.head

      logMessage.projectId shouldBe "pid"
      logMessage.functionId shouldBe "fid"
      logMessage.requestId shouldBe "rid"
      logMessage.id shouldNot be(empty)
      logMessage.status shouldBe "FAILURE"
      logMessage.timestamp shouldNot be(empty)
      logMessage.duration > 0 shouldBe true
      logMessage.message shouldBe a[JsObject]
      (logMessage.message \ "error").get.as[String] shouldNot be(empty)
    }

    "work off items and log a success message if the delivery was successful and returned a non-json body" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(200, "A plain response")
          .ignoreBody)

      withStubServer(stubs).withArg { server =>
        val webhook =
          Webhook(
            "pid",
            "fid",
            "rid",
            s"http://localhost:${server.port}/function-endpoint",
            "GIGAPIZZA",
            "someId",
            Map("X-Cheese-Header" -> "Gouda")
          )

        webhookPublisher.publish(webhook)
        logsConsumer.expectMsgCount(1)

        val logMessage: LogItem = logsConsumer.messages.head

        logMessage.projectId shouldBe "pid"
        logMessage.functionId shouldBe "fid"
        logMessage.requestId shouldBe "rid"
        logMessage.id shouldNot be(empty)
        logMessage.status shouldBe "SUCCESS"
        logMessage.timestamp shouldNot be(empty)
        logMessage.duration > 0 shouldBe true
        logMessage.message shouldBe a[JsObject]
        (logMessage.message \ "returnValue" \ "rawResponse").get.as[String] shouldBe "A plain response"
      }
    }

    "work off old mutation callbacks" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(200, "{}")
          .ignoreBody)

      withStubServer(stubs).withArg { server =>
        val messages = Seq(
          s"""{"projectId":"test-project-id","functionId":"","requestId":"","url":"http://localhost:${server.port}/function-endpoint","payload":"{\\\"createdNode\\\":{\\\"text\\\":\\\"a comment\\\",\\\"json\\\":[1,2,3]}}","id":"cj7c3vllp001nha58lxr6cx5b","headers":{}}""",
          s"""{"projectId":"test-project-id","functionId":"","requestId":"","url":"http://localhost:${server.port}/function-endpoint","payload":"{\\\"deletedNode\\\":{\\\"text\\\":\\\"UPDATED TEXT\\\",\\\"json\\\":{\\\"b\\\":[\\\"1\\\",2,{\\\"d\\\":3}]}}}","id":"cj7c3vv4b001rha58jh3otywr","headers":{}}""",
          s"""{"projectId":"test-project-id","functionId":"","requestId":"","url":"http://localhost:${server.port}/function-endpoint","payload":"{\\\"updatedNode\\\":{\\\"text\\\":\\\"UPDATED TEXT\\\",\\\"json\\\":{\\\"b\\\":[\\\"1\\\",2,{\\\"d\\\":3}]}},\\\"changedFields\\\":[\\\"text\\\"],\\\"previousValues\\\":{\\\"text\\\":\\\"some text created by nesting\\\"}}","id":"cj7c3vqed001pha58o7xculq1","headers":{}}""",
          s"""{"projectId":"test-project-id","functionId":"","requestId":"","url":"http://localhost:${server.port}/function-endpoint","payload":"{\\\"createdNode\\\":{\\\"text\\\":\\\"some text created by nesting\\\",\\\"json\\\":{\\\"b\\\":[\\\"1\\\",2,{\\\"d\\\":3}]}}}","id":"cj7c3vllp001mha58lh9kxikw","headers":{}}"""
        )

        messages.foreach(webhookPublisher.publishPlain("msg.0", _))
        logsConsumer.expectMsgCount(4, maxWait = 10.seconds)
        logsConsumer.messages.foreach(_.status shouldBe "SUCCESS")
      }
    }
  }
}
