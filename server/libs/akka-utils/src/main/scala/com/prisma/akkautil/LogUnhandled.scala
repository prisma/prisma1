package cool.graph.akkautil

import akka.actor.Actor.Receive

trait LogUnhandled { self =>
  private val className = self.getClass.getSimpleName

  def logUnhandled(receive: Receive): Receive = receive orElse {
    case x =>
      println(Console.RED + s"[$className] Received unknown message: $x" + Console.RESET)
  }

  def logAll(receive: Receive): Receive = {
    case x =>
      if (receive.isDefinedAt(x)) {
        receive(x)
        println(Console.GREEN + s"[$className] Handled message: $x" + Console.RESET)
      } else {
        println(Console.RED + s"[$className] Unhandled message: $x" + Console.RESET)
      }
  }
}
