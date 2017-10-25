package cool.graph.messagebus.testkits

import akka.testkit.TestProbe
import cool.graph.akkautil.SingleThreadedActorSystem
import cool.graph.bugsnag.BugSnagger
import cool.graph.messagebus.Conversions.{ByteMarshaller, ByteUnmarshaller}
import cool.graph.messagebus.Queue
import cool.graph.messagebus.QueueConsumer.ConsumeFn
import cool.graph.messagebus.queue.rabbit.{RabbitConsumerRef, RabbitQueuesRef}
import cool.graph.messagebus.queue.{BackoffStrategy, ConstantBackoff}
import cool.graph.messagebus.utils.RabbitUtils
import cool.graph.rabbit.Bindings.RoutingKey
import cool.graph.rabbit.Consumer

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.language.existentials
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

/**
  * Rabbit test kit for testing code that uses rabbit queueing.
  * This class is not intended to mirror the behaviour of the actual rabbit queue implementation.
  * It publishes and collects messages and allows reasoning over what messages should be received or not received.
  *
  * The API is similar to akka testkit, which it uses internally.
  *
  * >>> PLEASE NOTE: <<<
  * If queues are randomized (default false), it won't ack messages off off worker queues (doesn't interfere with regular
  * processing, meaning it only 'observes' messages on the error and main queue). Use randomization for fanout scenarios.
  *
  * However, a testkit doesn't work off messages unless 'start' is called.
  */
case class RabbitQueueTestKit[T](
    amqpUri: String,
    exchangeName: String,
    randomizeQueues: Boolean = false,
    backoff: BackoffStrategy = ConstantBackoff(1.second),
    exchangeDurable: Boolean = false
)(
    implicit tag: ClassTag[T],
    marshaller: ByteMarshaller[T],
    unmarshaller: ByteUnmarshaller[T]
) extends Queue[T] {

  implicit val system                 = SingleThreadedActorSystem("rabbitTestKit")
  implicit val bugSnagger: BugSnagger = null

  val probe                    = TestProbe()
  val errorProbe               = TestProbe()
  val logId                    = new java.util.Random().nextInt() // For log output correlation
  var messages: Vector[T]      = Vector.empty
  var errorMessages: Vector[T] = Vector.empty
  val exchange                 = RabbitUtils.declareExchange(amqpUri, exchangeName, 1, durable = exchangeDurable)

  val queues: RabbitQueuesRef = (for {
    queue <- exchange.channel.queueDeclare(exchangeName, randomizeName = randomizeQueues, durable = false, autoDelete = true)
    errQ  <- exchange.channel.queueDeclare(s"$exchangeName-error", randomizeName = randomizeQueues, durable = false, autoDelete = false)
    _     <- errQ.bindTo(exchange, RoutingKey("error.#"))
    _     <- queue.bindTo(exchange, RoutingKey("msg.#"))
  } yield RabbitQueuesRef(queue, errQ)) match {
    case Success(qs) => qs
    case Failure(e)  => sys.error(s"Unable to declare queues: $e")
  }

  val consumers: ArrayBuffer[Consumer] = ArrayBuffer[Consumer]()

  def withTestConsumers(): Unit = {
    queues.mainQ
      .consume { delivery =>
        val msg = unmarshaller(delivery.body)

        println(s"[TestKit][$logId] Received $msg")
        probe.ref ! msg
        messages.synchronized { messages = messages :+ msg }
        queues.mainQ.ack(delivery)
      }
      .getOrElse(sys.error("Can't declare main queue for test kit"))

    queues.errorQ
      .consume { delivery =>
        val msg = unmarshaller(delivery.body)

        println(s"[TestKit][$logId] Received errorMsg $msg")
        errorProbe.ref ! msg
        errorMessages.synchronized { errorMessages = errorMessages :+ msg }
        queues.errorQ.ack(delivery)
      }
      .getOrElse(sys.error("Can't declare error queue for test kit"))
  }

  override def withConsumer(fn: ConsumeFn[T]): RabbitConsumerRef = {
    val consumer = queues.mainQ.consume { delivery =>
      fn(unmarshaller(delivery.body))
    } match {
      case Success(c) => c
      case Failure(e) => sys.error(s"Unable to declare consumer: $e")
    }

    synchronized {
      consumers += consumer
    }

    RabbitConsumerRef(Seq(consumer))
  }

  /**
    * For expecting a specific message in the queue with given value in the main queue.
    */
  def expectMsg(msg: T, maxWait: FiniteDuration = 6.seconds): T = probe.expectMsg(maxWait, msg)

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
    * For expecting a single error message with given value in the error queue.
    */
  def expectErrorMsg(msg: T, maxWait: FiniteDuration = 6.seconds): T = errorProbe.expectMsg(maxWait, msg)

  /**
    * For expecting no error message in the given timeframe.
    */
  def expectNoErrorMsg(maxWait: FiniteDuration = 6.seconds): Unit = errorProbe.expectNoMsg(maxWait)

  /**
    * Expects a number of error messages to arrive in the error queue.
    */
  def expectErrorMsgCount[U: ClassTag](count: Int, maxWait: FiniteDuration = 6.seconds) = {
    errorProbe.expectMsgAllClassOf(maxWait, Array.fill(count)(tag.runtimeClass): _*)
    errorProbe.expectNoMsg(maxWait)
  }

  /**
    * Allows publishing of messages directly into the queues without any marshalling or routing key magic.
    * Useful for testing malformed messages or routing keys
    */
  def publishPlain(routingKey: String, message: String): Unit = exchange.publish(routingKey, message)

  /**
    * Regular publish of messages. Publishes to the main queue.
    */
  def publish(msg: T): Unit = exchange.publish("msg.0", marshaller(msg))

  /**
    * Regular publish of error messages. Publishes to the error queue.
    */
  def publishError(msg: T): Unit = exchange.publish("error.5", marshaller(msg))

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
  override def shutdown(): Unit = {
    purgeQueue(queues.mainQ.name)
    purgeQueue(queues.errorQ.name)

    consumers.foreach { c =>
      c.unsubscribe.getOrElse(println(s"Warn: Unable to unbind consumer: $c"))
    }

    Try { exchange.rabbitChannel.close() }

    Await.result(system.terminate(), 10.seconds)
  }

  def purgeQueue(name: String) = Try { exchange.rabbitChannel.queuePurge(name) } match {
    case Success(_)   => println(s"[TestKit] Purged queue $name")
    case Failure(err) => println(s"[TestKit] Failed to purge queue $name: $err")
  }

  def deleteQueue(name: String) = exchange.rabbitChannel.queueDelete(name)
}
