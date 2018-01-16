package cool.graph.messagebus.queue.inmemory

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.stream.ActorMaterializer
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.queue.inmemory.InMemoryQueueingMessages._
import cool.graph.messagebus.queue.{BackoffStrategy, LinearBackoff}
import cool.graph.messagebus.{ConsumerRef, Queue}

import scala.concurrent.duration._

/**
  * Queue implementation solely backed by actors, no external queueing stack is utilized.
  * Useful for the single server solution and tests.
  *
  * This is not yet a production ready implementation as robustness features for redelivery and ensuring
  * that an item is worked off are missing.
  */
case class InMemoryAkkaQueue[T](backoff: BackoffStrategy = LinearBackoff(5.seconds))(
    implicit val system: ActorSystem,
    materializer: ActorMaterializer
) extends Queue[T] {

  val router = system.actorOf(Props(RouterActor[T](backoff)))

  override def publish(msg: T): Unit = router ! Delivery(msg)

  override def shutdown: Unit = system.stop(router)

  override def withConsumer(fn: ConsumeFn[T]): ConsumerRef = {
    val worker = system.actorOf(Props(WorkerActor(router, fn)))

    router ! AddWorker(worker)
    ConsumerActorRef(worker)
  }
}

case class ConsumerActorRef(ref: ActorRef) extends ConsumerRef {
  override def stop: Unit = ref ! StopWork
}
