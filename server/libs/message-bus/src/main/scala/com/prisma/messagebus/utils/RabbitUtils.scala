package com.prisma.messagebus.utils

import com.prisma.errors.ErrorReporter
import com.prisma.rabbit.Import.{Exchange, Rabbit}

import scala.util.{Failure, Success}

object RabbitUtils {
  def declareExchange(amqpUri: String, exchangeName: String, concurrency: Int, durable: Boolean)(implicit reporter: ErrorReporter): Exchange = {
    val exchangeTry = for {
      channel <- Rabbit.channel(exchangeName, amqpUri, consumerThreads = concurrency)
      exDecl  <- channel.exchangeDeclare(s"$exchangeName-exchange", durable = durable)
    } yield exDecl

    exchangeTry match {
      case Success(ex) => ex
      case Failure(err) =>
        throw new Exception(s"Unable to declare rabbit exchange: $err", err)
    }
  }
}
