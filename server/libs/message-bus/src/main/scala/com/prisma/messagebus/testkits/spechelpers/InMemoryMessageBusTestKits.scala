package com.prisma.messagebus.testkits.spechelpers

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.testkit.TestKit
import com.prisma.messagebus.testkits.{InMemoryPubSubTestKit, InMemoryQueueTestKit}

import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

class InMemoryMessageBusTestKits(system: ActorSystem) extends TestKit(system) {
  implicit val actorSystem                     = system
  implicit val materializer: ActorMaterializer = ActorMaterializer()

  def withPubSubTestKit[T](checkFn: (InMemoryPubSubTestKit[T]) => Unit)(implicit tag: ClassTag[T]): Unit = {
    val testKit = InMemoryPubSubTestKit[T]()

    Try { checkFn(testKit) } match {
      case Success(_) => testKit.shutdown()
      case Failure(e) => testKit.shutdown(); throw e
    }
  }

  def withQueueTestKit[T](checkFn: (InMemoryQueueTestKit[T]) => Unit)(implicit tag: ClassTag[T]): Unit = {
    val testKit = InMemoryQueueTestKit[T]()

    Try { checkFn(testKit) } match {
      case Success(_) => testKit.shutdown()
      case Failure(e) => testKit.shutdown(); throw e
    }
  }

  def shutdownTestKit: Unit = {
    materializer.shutdown()
    shutdown(verifySystemShutdown = true)
  }
}
