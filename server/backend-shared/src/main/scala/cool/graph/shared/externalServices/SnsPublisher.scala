package cool.graph.shared.externalServices

import com.amazonaws.SdkClientException
import com.amazonaws.services.sns.AmazonSNS
import com.amazonaws.services.sns.model.{PublishRequest, PublishResult}
import cool.graph.cuid.Cuid
import scaldi.{Injectable, Injector}

trait SnsPublisher {
  def putRecord(payload: String): PublishResult
}

class SnsPublisherMock extends SnsPublisher {
  val messages = scala.collection.parallel.mutable.ParTrieMap[String, String]()

  def clearMessages = {
    messages.clear()
  }

  override def putRecord(payload: String): PublishResult = {
    messages.put(Cuid.createCuid(), payload)

    new PublishResult().withMessageId("0")
  }
}

class SnsPublisherImplementation(topic: String)(implicit inj: Injector) extends SnsPublisher with Injectable {

  val sns = inject[AmazonSNS](identified by "sns")

  override def putRecord(payload: String): PublishResult = {

    // todo: find a better way to handle this locally - perhaps with a docker based sns
    try {
      sns.publish(new PublishRequest(topic, payload))
    } catch {
      case e: SdkClientException => {
        println(e.getMessage)
        new PublishResult().withMessageId("999")
      }
    }
  }
}
