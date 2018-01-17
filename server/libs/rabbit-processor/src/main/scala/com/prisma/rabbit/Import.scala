package com.prisma.rabbit

object Import {
  val Rabbit = com.prisma.rabbit.Rabbit
  type Channel = com.prisma.rabbit.Channel
  val Channel = com.prisma.rabbit.Channel
  type Queue = com.prisma.rabbit.Queue
  val Queue = com.prisma.rabbit.Queue
  type Exchange = com.prisma.rabbit.Exchange
  val Exchange = com.prisma.rabbit.Exchange
  type Consumer = com.prisma.rabbit.Consumer
  val Consumer = com.prisma.rabbit.Consumer

  val ExchangeTypes = com.prisma.rabbit.ExchangeTypes
  val Bindings      = com.prisma.rabbit.Bindings
}
