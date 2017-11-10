package cool.graph.messagebus.pubsub

import akka.actor.{Actor, ActorRef, Terminated}
import akka.routing._
import cool.graph.messagebus.pubsub.PubSubProtocol.{Envelope, Publish, Subscribe, Unsubscribe}

import scala.collection.immutable

object PubSubProtocol {
  case class Subscribe(topic: String, actorRef: ActorRef)
  case class Publish(topic: String, message: Any)
  case class Unsubscribe(topic: String, ref: ActorRef)
  case class Envelope(actualTopic: String, message: Any)
}

case class PubSubRouter() extends Actor {
  val pubSubLogic = PubSubRoutingLogic()
  var router      = Router(pubSubLogic, Vector.empty)

  override def receive: Receive = {
    case Subscribe(topic, ref) =>
      context.watch(ref)
      router = router.addRoutee(PubSubRoutee(topic, ref))

    case Publish(topic, message) =>
      router.route(Envelope(topic, message), sender())

    case Unsubscribe(topic, ref) =>
      router = router.removeRoutee(PubSubRoutee(topic, ref))

    case Terminated(a) =>
      router = router.withRoutees(router.routees.collect {
        case routee @ PubSubRoutee(_, ref) if ref != a => routee
      })
  }
}

case class PubSubRoutee(topic: String, ref: ActorRef) extends Routee {
  override def send(message: Any, sender: ActorRef): Unit = {
    message match {
      case Envelope(_, payload) => ref.tell(payload, sender)
      case _                    =>
    }
  }
}

case class PubSubRoutingLogic() extends RoutingLogic {
  def select(message: Any, routees: immutable.IndexedSeq[Routee]): Routee = {
    val pubSubRoutees = routees.collect {
      case pubSubRoutee: PubSubRoutee => pubSubRoutee
    }

    message match {
      case Envelope(topic, _) =>
        val targets = pubSubRoutees.filter(_.topic == topic)
        SeveralRoutees(targets.asInstanceOf[immutable.IndexedSeq[Routee]])

      case _ =>
        NoRoutee
    }
  }
}
