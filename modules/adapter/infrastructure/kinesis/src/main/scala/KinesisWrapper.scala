import com.amazonaws.services.kinesis.AmazonKinesisClient
import com.amazonaws.services.kinesis.model.{PutRecordRequest, PutRecordResult}

import scala.util.Try

trait KinesisWrapper {

  protected val regionName: String
  protected val streamName: String

  protected lazy val kinesisClient =
    AmazonKinesisClient.builder().withRegion(regionName).build()

  def put(data: Array[Byte], partitionKey: String): Try[PutRecordResult] = Try {
    val request = new PutRecordRequest()
      .withStreamName(streamName)
      .withPartitionKey(partitionKey)
      .withData(java.nio.ByteBuffer.wrap(data))
    kinesisClient.putRecord(request)
  }
}
