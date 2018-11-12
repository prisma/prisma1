package com.prisma.messagebus.testkits

import com.prisma.errors.DummyErrorReporter
import com.prisma.messagebus.Conversions
import com.prisma.messagebus.pubsub.{Message, Only}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}
import play.api.libs.json.Json

class RabbitAkkaPubSubTestKitSpec extends WordSpecLike with Matchers with BeforeAndAfterAll with BeforeAndAfterEach with ScalaFutures {

  case class TestMessage(id: String, testOpt: Option[Int], testSeq: Seq[String])

  implicit val reporter          = DummyErrorReporter
  implicit val testMessageFormat = Json.format[TestMessage]
  implicit val testMarshaller    = Conversions.Marshallers.FromJsonBackedType[TestMessage]()
  implicit val testUnmarshaller  = Conversions.Unmarshallers.ToJsonBackedType[TestMessage]()

  val amqpUri = sys.env.getOrElse("RABBITMQ_URI", sys.error("RABBITMQ_URI required for testing"))
  val testRK  = Only("SomeRoutingKey")

  var testKit: RabbitAkkaPubSubTestKit[TestMessage] = _

  override def beforeEach = {
    testKit = RabbitAkkaPubSubTestKit[TestMessage](amqpUri, "test")
    testKit.start.futureValue
  }

  override def afterEach(): Unit = testKit.stop.futureValue

  "The rabbit pubsub testing kit" should {

    /**
      * Message expectation tests
      */
    "should expect a message correctly" in {
      val testMsg = TestMessage("someId1", None, Seq("1", "2"))

      testKit.publish(testRK, testMsg)
      testKit.expectMsg(Message[TestMessage](testRK.topic, testMsg))
    }

    "should blow up it expects a message and none arrives" in {
      val testMsg = TestMessage("someId2", None, Seq("1", "2"))

      an[AssertionError] should be thrownBy {
        testKit.expectMsg(Message[TestMessage](testRK.topic, testMsg))
      }
    }

    "should expect no message correctly" in {
      testKit.expectNoMsg()
    }

    "should blow up if no message was expected but one arrives" in {
      val testMsg = TestMessage("someId3", None, Seq("1", "2"))

      testKit.publish(testRK, testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectNoMsg()
      }
    }

    "should expect a message count correctly" in {
      val testMsg  = TestMessage("someId4", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId5", Some(123), Seq("2", "1"))

      testKit.publish(testRK, testMsg)
      testKit.publish(testRK, testMsg2)

      testKit.expectMsgCount(2)
    }

    "should blow up if it expects a message count and less arrive" in {
      val testMsg = TestMessage("someId6", None, Seq("1", "2"))

      testKit.publish(testRK, testMsg)

      an[AssertionError] should be thrownBy {
        testKit.expectMsgCount(2)
      }
    }

    "should blow up if it expects a message count and more arrive" in {
      val testMsg  = TestMessage("someId7", None, Seq("1", "2"))
      val testMsg2 = TestMessage("someId8", Some(123), Seq("2", "1"))

      testKit.publish(testRK, testMsg)
      testKit.publish(testRK, testMsg2)

      an[AssertionError] should be thrownBy {
        testKit.expectMsgCount(1)
      }
    }
  }
}
