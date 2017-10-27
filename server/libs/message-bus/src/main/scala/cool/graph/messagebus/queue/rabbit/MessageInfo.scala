package cool.graph.messagebus.queue.rabbit

case class MessageInfo(tries: Int, tryNextAt: Option[Long]) {
  def isDelayed: Boolean = {
    tryNextAt match {
      case Some(processingTime) =>
        val now = System.currentTimeMillis / 1000
        now < processingTime

      case None =>
        false
    }
  }

  // Messages start at 0 and have 5 tries. Tries are incremented after each unsuccessful processing.
  def exceededTries: Boolean = tries >= RabbitQueueConsumer.MAX_TRIES
}
