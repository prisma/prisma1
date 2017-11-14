package cool.graph.worker.workers

import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.akkautil.http.SimpleHttpClient
import cool.graph.messagebus.testkits.InMemoryQueueTestKit
import cool.graph.messagebus.testkits.spechelpers.InMemoryMessageBusTestKits
import cool.graph.stub.Import.withStubServer
import cool.graph.stub.StubDsl.Default.Request
import cool.graph.worker.payloads.{LogItem, Webhook}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.{JsObject, Json}

import scala.util.{Failure, Success, Try}

class WebhookDelivererWorkerSpec
    extends InMemoryMessageBusTestKits(SingleThreadedActorSystem("queueing-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterEach
    with BeforeAndAfterAll
    with ScalaFutures {
  import scala.concurrent.ExecutionContext.Implicits.global

  override def afterAll = shutdownTestKit

  def withWebhookWorker(checkFn: (WebhookDelivererWorker, InMemoryQueueTestKit[Webhook], InMemoryQueueTestKit[LogItem]) => Unit): Unit = {
    withQueueTestKit[LogItem] { logsTestKit =>
      withQueueTestKit[Webhook] { webhookTestKit =>
        val worker: WebhookDelivererWorker = WebhookDelivererWorker(SimpleHttpClient(), webhookTestKit, logsTestKit)

        worker.start.futureValue

        def teardown = {
          logsTestKit.shutdown()
          webhookTestKit.shutdown()
          worker.stop.futureValue
        }

        Try { checkFn(worker, webhookTestKit, logsTestKit) } match {
          case Success(_) => teardown
          case Failure(e) => teardown; throw e
        }
      }
    }
  }

  "The webhooks delivery worker" should {
    "work off items and log a success message if the delivery was successful" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(200, """{"data": "stuff", "logs": ["log1", "log2"]}""")
          .ignoreBody)

      withWebhookWorker { (webhookWorker, webhookTestKit, logsTestKit) =>
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

          webhookTestKit.publish(webhook)

          // Give the worker time to work off
          Thread.sleep(200)

          logsTestKit.expectPublishCount(1)

          val logMessage: LogItem = logsTestKit.messagesPublished.head

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
    }

    "work off items and log a failure message if the delivery was unsuccessful" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(400, """{"error": what are you doing?"}""")
          .ignoreBody)

      withWebhookWorker { (webhookWorker, webhookTestKit, logsTestKit) =>
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

          webhookTestKit.publish(webhook)
          logsTestKit.expectPublishCount(1)

          val logMessage: LogItem = logsTestKit.messagesPublished.head

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
    }

    "work off items and log a failure message if the delivery was unsuccessful due to the http call itself failing (e.g. timeout or not available)" in {
      withWebhookWorker { (webhookWorker, webhookTestKit, logsTestKit) =>
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

        webhookTestKit.publish(webhook)
        logsTestKit.expectPublishCount(1)

        val logMessage: LogItem = logsTestKit.messagesPublished.head

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
    }

    "work off items and log a success message if the delivery was successful and returned a non-json body" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(200, "A plain response")
          .ignoreBody)

      withWebhookWorker { (webhookWorker, webhookTestKit, logsTestKit) =>
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

          webhookTestKit.publish(webhook)
          logsTestKit.expectPublishCount(1)

          val logMessage: LogItem = logsTestKit.messagesPublished.head

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
    }

    "work off old mutation callbacks" in {
      val stubs = List(
        Request("POST", "/function-endpoint")
          .stub(200, "{}")
          .ignoreBody)

      withWebhookWorker { (webhookWorker, webhookTestKit, logsTestKit) =>
        withStubServer(stubs).withArg { server =>
          val webhook = Webhook(
            "test-project-id",
            "",
            "",
            s"http://localhost:${server.port}/function-endpoint",
            "{\\\"createdNode\\\":{\\\"text\\\":\\\"a comment\\\",\\\"json\\\":[1,2,3]}}",
            "cj7c3vllp001nha58lxr6cx5b",
            Map.empty
          )

          webhookTestKit.publish(webhook)
          logsTestKit.expectPublishCount(1)

          logsTestKit.messagesPublished.head.status shouldBe "SUCCESS"
        }
      }
    }
  }
}
