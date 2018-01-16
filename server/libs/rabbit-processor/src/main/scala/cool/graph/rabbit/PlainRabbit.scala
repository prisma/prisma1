package cool.graph.rabbit

import java.util.concurrent.{Executors, ThreadFactory}

import com.prisma.errors.ErrorReporter

import scala.util.Try
import com.rabbitmq.client.{ConnectionFactory, Channel => RabbitChannel}

object PlainRabbit {
  def connect(name: String, amqpUri: String, numberOfThreads: Int, qos: Option[Int])(implicit reporter: ErrorReporter): Try[RabbitChannel] = Try {

    val threadFactory: ThreadFactory = Utils.newNamedThreadFactory(name)
    val factory = {
      val f       = new ConnectionFactory()
      val timeout = sys.env.getOrElse("RABBIT_TIMEOUT_MS", "500").toInt
      f.setUri(amqpUri)
      f.setConnectionTimeout(timeout)
      f.setExceptionHandler(RabbitExceptionHandler(reporter))
      f.setThreadFactory(threadFactory)
      f.setAutomaticRecoveryEnabled(true)
      f
    }
    val executor   = Executors.newFixedThreadPool(numberOfThreads, threadFactory)
    val connection = factory.newConnection(executor)
    val theQos     = qos.orElse(sys.env.get("RABBIT_CHANNEL_QOS").map(_.toInt)).getOrElse(500)
    val chan       = connection.createChannel()
    chan.basicQos(theQos)
    chan
  }
}
