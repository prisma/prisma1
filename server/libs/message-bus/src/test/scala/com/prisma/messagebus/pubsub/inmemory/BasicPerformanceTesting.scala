package cool.graph.messagebus.pubsub.inmemory

import java.util.concurrent.atomic.AtomicInteger

import akka.actor.{Actor, ActorSystem, Props}
import akka.stream.ActorMaterializer
import cool.graph.messagebus.pubsub.PubSubProtocol.Publish
import cool.graph.messagebus.pubsub.{IntermediateCallbackActor, Message, PubSubRouterAlt, PubSubRouter}
import org.scalatest.WordSpecLike

import scala.reflect.ClassTag

class BasicPerformanceTesting extends WordSpecLike {

//  "Akka Routing implementation" should {
//    "perform on one topic" in {
//      performAllInOneTest[PubSubRouter]()
//    }
//
//    "perform on random topic" in {
//      performRandomTest[PubSubRouter]()
//    }
//  }
//
//  "Simple hash map implementation" should {
//    "perform on one topic" in {
//      performAllInOneTest[PubSubRouterAlt]()
//    }
//
//    "perform on random topic" in {
//      performRandomTest[PubSubRouterAlt]()
//    }
//  }

  def performAllInOneTest[T <: Actor]()(implicit tag: ClassTag[T]) = {
    implicit val system       = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val numOfSubscribers  = 10000
    val numOfMessages     = 1000
    val expectedResponses = numOfSubscribers * numOfMessages
    val responses         = new AtomicInteger(0)
    val router            = system.actorOf(Props[T])
    val topic             = "testTopic"

    // Set up X = numOfSubscribers subscribers
    for (i <- 0 until numOfSubscribers) {
      system.actorOf(Props(IntermediateCallbackActor[String, String](topic, router, { msg =>
        responses.incrementAndGet()
      })))
    }

    val startTime = System.currentTimeMillis()

    // Publish X = numOfMessages messages to the testTopic
    for (i <- 0 until numOfMessages) {
      router ! Publish(topic, Message[String](topic, "Hi there!"))
    }

    // Wait for completion
    while (responses.get() != expectedResponses) {
      println(s"${responses.get()} / $expectedResponses")
      Thread.sleep(100)
    }

    println(System.currentTimeMillis() - startTime)
  }

  def performRandomTest[T <: Actor]()(implicit tag: ClassTag[T]) = {
    implicit val system       = ActorSystem()
    implicit val materializer = ActorMaterializer()

    val numOfSubscribers  = 40000
    val numOfMessages     = 1000000
    val rand              = new java.util.Random()
    val expectedResponses = numOfMessages
    val responses         = new AtomicInteger(0)
    val router            = system.actorOf(Props[T])
    val baseTopic         = "testTopic"

    // Set up X = numOfSubscribers subscribers
    for (i <- 0 until numOfSubscribers) {
      system.actorOf(Props(IntermediateCallbackActor[String, String](s"$baseTopic-$i", router, { msg =>
        responses.incrementAndGet()
      })))
    }

    val startTime = System.currentTimeMillis()

    // Publish X = numOfMessages messages to the testTopic
    for (i <- 0 until numOfMessages) {
      val randTopic = s"$baseTopic-${rand.nextInt(numOfSubscribers)}"
      router ! Publish(randTopic, Message[String](randTopic, "Hi there!"))
    }

    // Wait for completion
    while (responses.get() != expectedResponses) {
      println(s"${responses.get()} / $numOfMessages")
      Thread.sleep(100)
    }

    println(System.currentTimeMillis() - startTime)
  }
}
