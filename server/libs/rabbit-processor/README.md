## Rabbit-Processor-Scala
=======================

This library is intended to help when working with RabbitMQ. It provides a layer above the original RabbitMQ Java client.

You can find the release notes for the different versions [here](release-notes.md).

## Design goals of this library

* Containerize Exceptions instead of throwing them. The original RabbitMQ makes heavy use of throwing. The user of this library is not required to manually try/catch exceptions, which is error prone. Instead the Monad `Try[T]` is used.
* Core concepts of Rabbit shall be represented as proper types, e.g.:
    * The concepts `Queue` and `Exchange` are represented as types. 
    * Binding rules are explicitly modeled as types instead of opaque routing keys.
* Capture best practices around configurations of stuff like thread pools and connections. 

## How to use

Add the library to your project via your `build.sbt`. Find the latest version in the [release notes](release-notes.md).

```scala
libraryDependencies ++= Seq(
  "cool.graph" %% "rabbit-processor-scala" % "<version>"
)
```

You can import all the stuff you need like this:
```scala
import cool.graph.rabbit.Import._
import cool.graph.rabbit.Import.Bindings._
```

As all "dangerous" method calls return a `Try[T]` it is sensible to make heavy use of for comprehensions.

### How to declare a Queue or Exchange

At first you need to create a channel instance to interact with RabbitMQ. Once you have a Channel, you can use it to use create a `Queue` or an `Exchange`.

```scala
for {
  channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
  queue    <- channel.queueDeclare(queueName, durable = false, autoDelete = true)
  exchange <- channel.exchangeDeclare("some-exchange", durable = false)
  ...
} yield ()
```

### How to bind a Queue to an Exchange

To bind a `Queue` to exchange you need to call the `bindTo` method and pass an exchange. You may either pass an `Exchange` object or a `String` with the name to that method. Additionally you must pass a `Binding` object to that method. There are different ones available, but the important ones are probably `Fanout` and `RoutingKey(String)`.

```scala
for {
  channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
  queue    <- channel.queueDeclare(queueName, durable = false, autoDelete = true)
  exchange <- channel.exchangeDeclare("some-exchange", durable = false)
  _        <- queue.bindTo(exchange, FanOut) // or: channel.bindTo("some-exchange", FanOut)
  ...
} yield ()
```

If you wonder what the `_` is used for: It just means that we are not interested in the returned result of the method call.

### How to consume from a Queue

Install a consumer by calling the `consume` method on a `Queue`. You need to provide a function that receives a `Delivery` object, which contains the body and envelope.  Once you have processed the message you need to cal `ack` or `nack` on the `Queue`. You may either pass the `Delivery` or a `Long` (the delivery tag) to `ack` and `nack`. 
If the passed the consuming function throws an exception, it will be automatically reported via BugSnag. 

```scala
for {
  ...
  queue    <- channel.queueDeclare(queueName, durable = false, autoDelete = true)
  ...
  _        <- queue.consume { delivery =>
    // do something with the delivery and ack afterwards
    println(delivery.body)
    queue.ack(delivery)
  }
} yield ()
```

### How to consume with multiple consumers

The only difference to the previous is that you need to provide an additional argument to the `consume` method on `Queue`. If you install more than consumer you should have as many consumer threads on the channel as total consumers. Check below how the number is passed both to the `channel` and the `consume` method.

```scala
val numberOfConsumers = 4
for {
  channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = numberOfConsumers)
  queue    <- channel.queueDeclare(queueName, durable = false, autoDelete = true)
  ...
  _        <- queue.consume(numberOfConsumers){ delivery =>
    // do something with the delivery and ack afterwards
    println(delivery.body)
    queue.ack(delivery)
  }
} yield ()
```

### How to publish to an Exchange

Publishing is actually not different. But you might want to do the setup code into a separate method so that you can call publish on the object repeatedly. You then call this method once and just call the `publish` method on the `Exchange`.  

```scala
def setupMyCustomExchange: Exchange = {
  val exchange: Try[Exchange] = for {
    channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
    exchange <- channel.exchangeDeclare("some-exchange", durable = false)
  } yield exchange
  exchange match {
    case Success(x) =>
      x
    case Failure(e) =>
      // maybe do something to retry. A naive way could look like this:
      Thread.sleep(1000)
      setupMyCustomExchange
  }
}
val exchange = setupMyCustomExchange
exchange.publish("routingKey", "some message")
```

### How to publish directly to a Queue

```scala
def setupMyQueue: Queue = {
  val queue: Try[Queue] = for {
    channel  <- Rabbit.channel(queueName, amqpUri, consumerThreads = 1)
    queue    <- channel.queueDeclare("my-queue", durable = false, autoDelete = true)
  } yield queue
  queue match {
    case Success(x) =>
      x
    case Failure(e) =>
      // maybe do something to retry. A naive way could look like this:
      Thread.sleep(1000)
      setupMyQueue
  }
}
val queue = setupMyQueue
queue.publish("some message")
```
