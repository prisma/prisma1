package com.prisma.messagebus.testkits

import com.prisma.errors.DummyErrorReporter
import com.prisma.messagebus.Conversions
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.Json

class RabbitQueueTestKitSpec extends WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures {

  case class TestMessage(id: String, testOpt: Option[Int], testSeq: Seq[String])

  implicit val reporter          = DummyErrorReporter
  implicit val testMessageFormat = Json.format[TestMessage]
  implicit val testMarshaller    = Conversions.Marshallers.FromJsonBackedType[TestMessage]()
  implicit val testUnmarshaller  = Conversions.Unmarshallers.ToJsonBackedType[TestMessage]()

  val amqpUri = sys.env.getOrElse("RABBITMQ_URI", sys.error("RABBITMQ_URI required for testing"))

  var testKit: RabbitQueueTestKit[TestMessage] = _

  override def beforeEach = {
    testKit = RabbitQueueTestKit[TestMessage](amqpUri, "test")
    testKit.withTestConsumers()
  }

  override def afterEach(): Unit = testKit.shutdown()

  "The rabbit queue testing kit" should {

    /**
      * Message expectation tests
      */
    "should expect a message correctly" in {
      val testMsg = TestMessage("someId1", None, Seq("1", "2"))

      testKit.publish(testMsg)
      testKit.expectMsg(testMsg)
    }

    "should blow up it expects a message and none arrives" in {
      val testMsg = TestMessage("someId2", None, Seq("1", "2"))

      an[AssertionError] should be thrownBy {
        testKit.expectMsg(testMsg)
      }
    }

    "should expect no message correctly" in {
      testKit.expectNoMsg()
    }

    "should blow up if no message was expected but one arrives" in {
      val testMsg = TestMessage("someId3", None, Seq("1", "2"))

      testKit.publish(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectNoMsg()
      }
    }

    "should expect a message count correctly" in {
      val testMsg  = TestMessage("someId4", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId5", Some(123), Seq("2", "1"))

      testKit.publish(testMsg)
      testKit.publish(testMsg2)

      testKit.expectMsgCount(2)
    }

    "should blow up if it expects a message count and less arrive" in {
      val testMsg = TestMessage("someId6", None, Seq("1", "2"))

      testKit.publish(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectMsgCount(2)
      }
    }

    "should blow up if it expects a message count and more arrive" in {
      val testMsg  = TestMessage("someId7", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId8", Some(123), Seq("2", "1"))

      testKit.publish(testMsg)
      testKit.publish(testMsg2)

      an[AssertionError] should be thrownBy {
        testKit.expectMsgCount(1)
      }
    }

    /**
      * Error msg expectation tests
      */
    "should expect an error message correctly" in {
      val testMsg = TestMessage("someId9", None, Seq("1", "2"))

      testKit.publishError(testMsg)
      testKit.expectErrorMsg(testMsg)
    }

    "should blow up it expects an error message and none arrives" in {
      val testMsg = TestMessage("someId10", None, Seq("1", "2"))

      an[AssertionError] should be thrownBy {
        testKit.expectErrorMsg(testMsg)
      }
    }

    "should expect no error message correctly" in {
      testKit.expectNoErrorMsg()
    }

    "should blow up if no error message was expected but one arrives" in {
      val testMsg = TestMessage("someId11", None, Seq("1", "2"))

      testKit.publishError(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectNoErrorMsg()
      }
    }

    "should expect an error message count correctly" in {
      val testMsg  = TestMessage("someId12", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId13", Some(123), Seq("2", "1"))

      testKit.publishError(testMsg)
      testKit.publishError(testMsg2)

      testKit.expectErrorMsgCount(2)
    }

    "should blow up if it expects an error message count and less arrive" in {
      val testMsg = TestMessage("someId14", None, Seq("1", "2"))

      testKit.publishError(testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectErrorMsgCount(2)
      }
    }

    "should blow up if it expects an error message count and more arrive" in {
      val testMsg  = TestMessage("someId15", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId16", Some(123), Seq("2", "1"))

      testKit.publishError(testMsg)
      testKit.publishError(testMsg2)

      an[AssertionError] should be thrownBy {
        testKit.expectErrorMsgCount(1)
      }
    }
  }
}
