package cool.graph.shared.externalServices

import java.nio.ByteBuffer
import com.amazonaws.services.kinesis.AmazonKinesis
import com.amazonaws.services.kinesis.model.PutRecordResult
import cool.graph.cuid.Cuid
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

trait KinesisPublisher {
  def putRecord(payload: String, shardId: String = "0"): PutRecordResult
  def healthCheck: Future[Unit]
}

case class DummyKinesisPublisher() extends KinesisPublisher {
  def putRecord(payload: String, shardId: String = "0"): PutRecordResult = new PutRecordResult().withSequenceNumber("0").withShardId("0")

  def healthCheck: Future[Unit] = Future.successful(())
}

class KinesisPublisherMock extends KinesisPublisher {
  val messages = scala.collection.mutable.Map.empty[String, String]

  def clearMessages = messages.clear()

  def putRecord(payload: String, shardId: String = "0"): PutRecordResult = {
    messages.put(Cuid.createCuid(), payload)

    new PutRecordResult().withSequenceNumber("0").withShardId("0")
  }

  def healthCheck: Future[Unit] = Future.successful(())
}

class KinesisPublisherImplementation(streamName: String, kinesis: AmazonKinesis) extends KinesisPublisher {
  def putRecord(payload: String, shardId: String = "0"): PutRecordResult = {
    kinesis.putRecord(streamName, ByteBuffer.wrap(payload.getBytes()), shardId)
  }

  def healthCheck: Future[Unit] = Future {
    try { kinesis.listStreams() } catch {
      case e: com.amazonaws.services.kinesis.model.LimitExceededException =>
    }
  }
}
