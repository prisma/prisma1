package cool.graph.akkautil.specs2

import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.specs2.mutable.After

import scala.concurrent.Await

object TestConfig {
  val config = ConfigFactory.parseString("""
      |akka {
      |  log-dead-letters = 1
      |}
      |
    """.stripMargin)
}

/**
  * This class is a context for specs2, which allows the usage of the TestKit provided by Akka
  * to test actor systems.
  */
class AkkaTestKitSpecs2Context extends TestKit(ActorSystem("test-system", TestConfig.config)) with ImplicitSender with After {

  import scala.concurrent.duration._

  def after = Await.result(system.terminate(), 10.seconds)
}
