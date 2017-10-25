package cool.graph.shared.externalServices

import java.nio.ByteBuffer

import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.PutRecordResult
import cool.graph.cuid.Cuid
import scaldi.{Injectable, Injector}

import scala.collection.parallel.mutable.ParTrieMap

trait KinesisPublisher {
  def putRecord(payload: String, shardId: String = "0"): PutRecordResult
}

class KinesisPublisherMock extends KinesisPublisher {
  val messages = scala.collection.mutable.Map.empty[String, String]

  def clearMessages = messages.clear()

  override def putRecord(payload: String, shardId: String = "0"): PutRecordResult = {
    messages.put(Cuid.createCuid(), payload)

    new PutRecordResult().withSequenceNumber("0").withShardId("0")
  }
}

class KinesisPublisherImplementation(streamName: String, kinesis: AmazonKinesis) extends KinesisPublisher with Injectable {

  override def putRecord(payload: String, shardId: String = "0"): PutRecordResult = {
    kinesis.putRecord(streamName, ByteBuffer.wrap(payload.getBytes()), shardId)
  }
}
