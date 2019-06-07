import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.sqs.AmazonSQSClient
import org.scalatest.FunSuite

class SQSWrapperTest extends FunSuite with SQSWrapper {
  override val regionName = "ap-northeast-1"
  override val queueName = "lspan-sqs"
  override lazy val sqsClient =
    AmazonSQSClient
      .builder()
      .withCredentials(
        new AWSStaticCredentialsProvider(
          new BasicAWSCredentials("x", "x:)))
      .withEndpointConfiguration(
        new EndpointConfiguration("http://localhost:4576",
                                  Regions.AP_NORTHEAST_1.getName))
      .build()

  test("queを取得する") {
    println(receiveMessage)
  }
}
