package cool.graph.messagebus.queue.inmemory

import akka.actor.{Actor, ActorRef, Terminated}
import akka.routing.{ActorRefRoutee, RoundRobinRoutingLogic, Router}
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.queue.BackoffStrategy
import cool.graph.messagebus.queue.inmemory.InMemoryQueueingMessages._

/**
  * Todos
  * - Message protocol? ACK / NACK etc.?
  * - Queue actor supervising message ack / nack => actor ask with timeout?
  * - Catching deadletters and requeueing items?
  */
object InMemoryQueueingMessages {
  case class AddWorker(ref: ActorRef)
  object StopWork

  case class Delivery[T](payload: T, tries: Int = 0) {
    def nextTry: Delivery[T] = copy(tries = tries + 1)
  }

  case class DeferredDelivery[T](item: Delivery[T])
}

case class RouterActor[T](backoff: BackoffStrategy) extends Actor {
  import context.dispatcher

  var router = Router(RoundRobinRoutingLogic(), Vector.empty)

  override def receive = {
    case AddWorker(ref: ActorRef) =>
      context watch ref
      router = router.addRoutee(ActorRefRoutee(ref))

    case item: Delivery[T] =>
      router.route(item, sender())

    case deferred: DeferredDelivery[T] =>
      val dur = BackoffStrategy.backoffDurationFor(deferred.item.tries, backoff)
      BackoffStrategy.backoff(dur).map(_ => router.route(deferred.item, sender()))

    case Terminated(a) =>
      // todo: Restart worker actor if terminated abnormally?
      router = router.removeRoutee(a)
  }
}

case class WorkerActor[T](router: ActorRef, fn: ConsumeFn[T]) extends Actor {
  import context.dispatcher

  override def receive = {
    case i: Delivery[T] =>
      if (i.tries < 5) {
        fn(i.payload).onFailure {
          case _ => router ! DeferredDelivery(i.nextTry)
        }
      } else {
        println(s"Discarding message, tries exceeded: $i")
      }

    case StopWork =>
      context.stop(self)
  }
}
