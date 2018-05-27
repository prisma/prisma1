package com.prisma.shared.models

sealed trait Function {
  def name: String
  def isActive: Boolean
  def delivery: FunctionDelivery
  def typeCode: FunctionType.Value
}

object FunctionType extends Enumeration {
  val ServerSideSubscription = Value("server-side-subscription")
}

case class ServerSideSubscriptionFunction(
    name: String,
    isActive: Boolean,
    delivery: FunctionDelivery,
    query: String
) extends Function {
  override def typeCode = FunctionType.ServerSideSubscription
}

sealed trait FunctionDelivery {
  def typeCode: FunctionDeliveryType.Value
}

object FunctionDeliveryType extends Enumeration {
  val WebhookDelivery = Value("webhook-delivery")
}

case class WebhookDelivery(
    url: String,
    headers: Vector[(String, String)]
) extends FunctionDelivery {
  override def typeCode = FunctionDeliveryType.WebhookDelivery
}
