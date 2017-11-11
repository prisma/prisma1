package cool.graph.messagebus.queue.inmemory

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.{TestKit, TestProbe}
import cool.graph.messagebus.QueuePublisher
import cool.graph.messagebus.queue.{BackoffStrategy, ConstantBackoff}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach, Matchers, WordSpecLike}

import scala.concurrent.Future
import scala.concurrent.duration._

class InMemoryAkkaQueueSpec
    extends TestKit(ActorSystem("queueing-spec"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll
    with BeforeAndAfterEach
    with ScalaFutures {

  implicit val materializer = ActorMaterializer()

  def withInMemoryQueue[T](backoff: BackoffStrategy = ConstantBackoff(100.millis))(testFn: (InMemoryAkkaQueue[T], TestProbe) => Unit) = {
    val inMemoryQueue = InMemoryAkkaQueue[T](backoff)
    val testProbe     = TestProbe()

    try {
      testFn(inMemoryQueue, testProbe)
    } finally {
      inMemoryQueue.shutdown
    }
  }

  override def afterAll = shutdown(verifySystemShutdown = true)

  "Queue" should {
    "call the onMsg function if a valid message arrives" in {
      withInMemoryQueue[String]() { (queue, probe) =>
        queue.withConsumer((str: String) => { probe.ref ! str; Future.successful(()) })
        queue.publish("test")
        probe.expectMsg("test")
      }
    }

    "increment the message tries correctly on failure" in {
      withInMemoryQueue[String]() { (queue, probe) =>
        queue.withConsumer((str: String) => { probe.ref ! str; Future.failed(new Exception("Kabooom")) })
        queue.publish("test")

        // 5 tries, 5 times the same message (can't check for the tries explicitly here)
        probe.expectMsgAllOf(2.seconds, Vector.fill(5) { "test" }: _*)
        probe.expectNoMsg(1.second)
      }
    }

    "map a type correctly with a MappingQueueConsumer" in {
      withInMemoryQueue[String]() { (queue, probe) =>
        val mapped = queue.map[Int]((str: String) => str.toInt)

        mapped.withConsumer((int: Int) => { probe.ref ! int; Future.successful(()) })
        queue.publish("123")

        probe.expectMsg(123)
      }
    }

    "map a type correctly with a MappingQueuePublisher" in {
      withInMemoryQueue[String]() { (queue: InMemoryAkkaQueue[String], probe) =>
        val mapped: QueuePublisher[Int] = queue.map[Int]((int: Int) => int.toString)

        queue.withConsumer((str: String) => { probe.ref ! str; Future.successful(()) })
        mapped.publish(123)

        probe.expectMsg("123")
      }
    }
  }
}
