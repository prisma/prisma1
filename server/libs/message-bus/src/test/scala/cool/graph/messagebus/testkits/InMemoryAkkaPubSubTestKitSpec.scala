package cool.graph.messagebus.testkits

import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import cool.graph.akkautil.SingleThreadedActorSystem
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import org.scalatest.concurrent.ScalaFutures

class InMemoryAkkaPubSubTestKitSpec
    extends TestKit(SingleThreadedActorSystem("pubsub-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {

  case class TestMessage(id: String, testOpt: Option[Int], testSeq: Seq[String])

  var testKit: InMemoryQueueTestKit[TestMessage] = _
  implicit val materializer: ActorMaterializer   = ActorMaterializer()

  override def beforeEach = testKit = InMemoryQueueTestKit[TestMessage]()
  override def afterEach  = testKit.shutdown()

  override def afterAll = {
    materializer.shutdown()
    shutdown(verifySystemShutdown = true)
  }

  "The in-memory queue testing kit" should {

    /**
      * Incoming messages expectation tests
      */
    "should expect an incoming message correctly" in {
      val testMsg = TestMessage("someId1", None, Seq("1", "2"))

      testKit.withTestConsumer()
      testKit.publish(testMsg)
      testKit.expectMsg(testMsg)
      testKit.messagesReceived.length shouldEqual 1
    }

    "should blow up it expects a message and none arrives" in {
      val testMsg = TestMessage("someId2", None, Seq("1", "2"))

      testKit.withTestConsumer()

      an[AssertionError] should be thrownBy {
        testKit.expectMsg(testMsg)
      }
    }

    "should expect no message correctly" in {
      testKit.withTestConsumer()
      testKit.expectNoMsg()
    }

    "should blow up if no message was expected but one arrives" in {
      val testMsg = TestMessage("someId3", None, Seq("1", "2"))

      testKit.withTestConsumer()
      testKit.publish(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectNoMsg()
      }
    }

    "should expect a message count correctly" in {
      val testMsg  = TestMessage("someId4", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId5", Some(123), Seq("2", "1"))

      testKit.withTestConsumer()
      testKit.publish(testMsg)
      testKit.publish(testMsg2)
      testKit.expectMsgCount(2)
      testKit.messagesReceived.length shouldEqual 2
    }

    "should blow up if it expects a message count and less arrive" in {
      val testMsg = TestMessage("someId6", None, Seq("1", "2"))

      testKit.withTestConsumer()
      testKit.publish(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectMsgCount(2)
      }
    }

    "should blow up if it expects a message count and more arrive" in {
      val testMsg  = TestMessage("someId7", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId8", Some(123), Seq("2", "1"))

      testKit.withTestConsumer()
      testKit.publish(testMsg)
      testKit.publish(testMsg2)

      an[AssertionError] should be thrownBy {
        testKit.expectMsgCount(1)
      }
    }

    /**
      * Published messages expectation tests
      */
    "should expect a published message correctly" in {
      val testMsg = TestMessage("someId1", None, Seq("1", "2"))

      testKit.publish(testMsg)
      testKit.expectPublishedMsg(testMsg)
      testKit.messagesPublished.length shouldEqual 1
    }

    "should blow up it expects a published message and none arrives" in {
      val testMsg = TestMessage("someId2", None, Seq("1", "2"))

      an[AssertionError] should be thrownBy {
        testKit.expectPublishedMsg(testMsg)
      }
    }

    "should expect no published message correctly" in {
      testKit.expectNoPublishedMsg()
    }

    "should blow up if no published message was expected but one arrives" in {
      val testMsg = TestMessage("someId3", None, Seq("1", "2"))

      testKit.publish(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectNoPublishedMsg()
      }
    }

    "should expect a published message count correctly" in {
      val testMsg  = TestMessage("someId4", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId5", Some(123), Seq("2", "1"))

      testKit.publish(testMsg)
      testKit.publish(testMsg2)
      testKit.expectPublishCount(2)
      testKit.messagesPublished.length shouldEqual 2
    }

    "should blow up if it expects a published message count and less arrive" in {
      val testMsg = TestMessage("someId6", None, Seq("1", "2"))

      testKit.publish(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectPublishCount(2)
      }
    }

    "should blow up if it expects a published message count and more arrive" in {
      val testMsg  = TestMessage("someId7", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId8", Some(123), Seq("2", "1"))

      testKit.publish(testMsg)
      testKit.publish(testMsg2)

      an[AssertionError] should be thrownBy {
        testKit.expectPublishCount(1)
      }
    }
  }
}
