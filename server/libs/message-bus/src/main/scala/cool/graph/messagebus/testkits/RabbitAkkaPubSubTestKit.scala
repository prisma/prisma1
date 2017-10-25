package cool.graph.messagebus.testkits

import akka.actor.ActorRef
import akka.testkit.TestProbe
import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller, Converter}
import cool.graph.messagebus.PubSub
import cool.graph.messagebus.pubsub.{Message, Only, Subscription, Topic}
import cool.graph.messagebus.utils.RabbitUtils
import cool.graph.rabbit
import cool.graph.rabbit.Bindings.RoutingKey
import cool.graph.rabbit.Consumer

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Rabbit test kit for testing code that uses rabbit pub sub.
  * This class is not intended to mirror the behaviour of the actual pub sub implementation.
  * It publishes and collects messages and allows reasoning over what messages should be received or not received.
  *
  * The API is similar to akka testkit, which it uses internally.
  *
  * >>> PLEASE NOTE: <<<
  * If queues are randomized (default false), it won't ack messages off off queues (doesn't interfere with regular
  * processing, meaning it only 'observes' messages on the queue). Use randomization for fanout scenarios.
  *
  * However, a testkit doesn't start ack'ing off messages unless 'start' is called.
  */
case class RabbitAkkaPubSubTestKit[T](
    amqpUri: String,
    exchangeName: String,
    randomizeQueues: Boolean = false,
    exchangeDurable: Boolean = false
)(
    implicit tag: ClassTag[Message[T]],
    marshaller: ByteMarshaller[T],
    unmarshaller: ByteUnmarshaller[T]
) extends PubSub[T] {

  implicit val system                 = SingleThreadedActorSystem("rabbitPubSubTestKit")
  implicit val bugSnagger: BugSnagger = null

  val probe                        = TestProbe()
  val logId                        = new java.util.Random().nextInt() // For log output correlation
  var messages: Vector[Message[T]] = Vector.empty
  var queueDef: rabbit.Queue       = _
  val exchange                     = RabbitUtils.declareExchange(amqpUri, exchangeName, 1, durable = exchangeDurable)

  lazy val consumer: Try[Consumer] = for {
    queue <- exchange.channel.queueDeclare(exchangeName, randomizeName = randomizeQueues, durable = false, autoDelete = true)
    _     <- queue.bindTo(exchange, RoutingKey("#"))
    qConsumer <- queue.consume { delivery =>
                  val msg = Message(delivery.envelope.getRoutingKey, unmarshaller(delivery.body))
                  println(s"[PubSub-TestKit][$logId] Received $msg")

                  probe.ref ! msg
                  messages.synchronized { messages = messages :+ msg }

                  queue.ack(delivery)
                }
  } yield {
    queueDef = queue
    qConsumer
  }

  /**
    * For expecting a specific message in the queue with given value in the queue.
    */
  def expectMsg(msg: Message[T], maxWait: FiniteDuration = 6.seconds): Message[T] = probe.expectMsg[Message[T]](maxWait, msg)

  /**
    * For expecting no message in the given timeframe.
    */
  def expectNoMsg(maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectNoMsg(maxWait)
  }

  /**
    * Expects a number of messages to arrive in the main queue.
    * Matches the total count received in the time frame, so too many messages is also a failure.
    */
  def expectMsgCount(count: Int, maxWait: FiniteDuration = 6.seconds): Unit = {
    probe.expectMsgAllClassOf(maxWait, Array.fill(count)(tag.runtimeClass): _*)
    probe.expectNoMsg(maxWait)
  }

  /**
    * Publishes the given message using the supplied routing key.
    */
  def publish(routingKey: String, message: String): Unit = exchange.publish(routingKey, message)

  /**
    * Start the test kit consumer. Waits a bit before returning as a safety buffer to prevent subsequent test publish calls
    * to be too fast for the consumer to bind and consume in time.
    */
  def start = {
    val bootFuture = Future.fromTry(consumer)

    Thread.sleep(500)
    bootFuture
  }

  /**
    * Stops the test kit. Unbinds all consumers, purges all queues. DOES NOT DELETE ANYTHING on the Rabbit.
    * Why? If code that is tested has, for example, a queue in a singleton object, then deleting the exchange
    * will gracefully shut down the rabbit channel, which then won't be recovered by the rabbit lib and subsequently
    * cause weirdness in the code and tests. Purging and not deleting has the downside that the will be exchanges and
    * queues dangling around, but as we use containers for testing this is not an issue.
    *
    * There are ways of dealing with that in code of course, but at the cost of readability and complexity.
    * If you need to delete queues explicitly use the appropriate methods.
    */
  def stop = {
    purgeQueue(queueDef.name)

    Try { consumer.get.unsubscribe.get } match {
      case Failure(err) => println(err)
      case _            =>
    }

    Try { exchange.rabbitChannel.close() }

    system.terminate()
  }

  def purgeQueue(queue: String) = Try { exchange.rabbitChannel.queuePurge(queue) } match {
    case Success(_)   => println(s"[PubSub-TestKit] Purged queue $queue")
    case Failure(err) => println(s"[PubSub-TestKit] Failed to purge queue $queue: $err")
  }

  def deleteQueue(name: String) = exchange.rabbitChannel.queueDelete(name)

  override def publish(topic: Only, msg: T): Unit = exchange.publish(topic.topic, marshaller(msg))

  override def subscribe(topic: Topic, onReceive: (Message[T]) => Unit): Subscription       = ???
  override def subscribe(topic: Topic, subscriber: ActorRef): Subscription                  = ???
  override def unsubscribe(subscription: Subscription): Unit                                = ???
  override def subscribe[U](topic: Topic, subscriber: ActorRef, converter: Converter[T, U]) = ???
}
