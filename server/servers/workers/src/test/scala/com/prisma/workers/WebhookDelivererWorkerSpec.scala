package com.prisma.workers

import com.prisma.akkautil.SingleThreadedActorSystem
import com.prisma.akkautil.http.SimpleHttpClient
import com.prisma.messagebus.testkits.InMemoryQueueTestKit
import com.prisma.messagebus.testkits.spechelpers.InMemoryMessageBusTestKits
import com.prisma.stub.Import.withStubServer
import com.prisma.stub.StubDsl.Default.Request
import com.prisma.workers.payloads.Webhook
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

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

  def withWebhookWorker(checkFn: (WebhookDelivererWorker, InMemoryQueueTestKit[Webhook]) => Unit): Unit = {
    withQueueTestKit[Webhook] { webhookTestKit =>
      val worker: WebhookDelivererWorker = WebhookDelivererWorker(SimpleHttpClient(), webhookTestKit)

      worker.start.futureValue

      def teardown = {
        webhookTestKit.shutdown()
        worker.stop.futureValue
      }

      Try { checkFn(worker, webhookTestKit) } match {
        case Success(_) => teardown
        case Failure(e) => teardown; throw e
      }
    }
  }

  "The webhooks delivery worker" should {
    "work off items" in {
      val stub = Request("POST", "/function-endpoint")
        .stub(200, """{"data": "stuff", "logs": ["log1", "log2"]}""")
        .ignoreBody

      withWebhookWorker { (webhookWorker, webhookTestKit) =>
        withStubServer(List(stub)).withArg { server =>
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
          Thread.sleep(2000)

          server.requestCount(stub) should equal(1)
          val lastRequest = server.lastRequest
          lastRequest.httpMethod should equal("POST")
          lastRequest.body should equal(webhook.payload)
          lastRequest.headers should contain("X-Cheese-Header" -> "Gouda")
          lastRequest.path should equal("/function-endpoint")
        }
      }
    }

    "work off old mutation callbacks" in {
      val stub = Request("POST", "/function-endpoint")
        .stub(200, "{}")
        .ignoreBody

      withWebhookWorker { (webhookWorker, webhookTestKit) =>
        withStubServer(List(stub)).withArg { server =>
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

          Thread.sleep(2000)
          server.requestCount(stub) should equal(1)
        }
      }
    }
  }
}
