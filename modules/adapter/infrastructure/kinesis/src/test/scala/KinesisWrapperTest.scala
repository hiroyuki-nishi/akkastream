import java.nio.charset.StandardCharsets

import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration
import com.amazonaws.regions.Regions
import com.amazonaws.services.kinesis.AmazonKinesisClient
import org.scalatest.FunSuite
import spray.json.{JsObject, JsString}

import scala.util.Try

class KinesisWrapperTest extends FunSuite {

  private def generateData: Try[String] = Try(
    JsObject(
      "data" -> JsString("test")
    ).compactPrint
  )

  test("testPut") {
    new Fixture {
      for {
        data <- generateData
        json <- Try(data.getBytes(StandardCharsets.UTF_8))
        _ <- put(json, "logEvent")
      } yield ()
    }
  }

  trait Fixture extends KinesisWrapper {
    override val regionName: String = "ap-northeast-1"
    override val streamName: String = "lspan-spa-kinesis"
    override lazy val kinesisClient =
      AmazonKinesisClient
        .builder()
        .withCredentials(
          new AWSStaticCredentialsProvider(new BasicAWSCredentials("x", "x")))
        .withEndpointConfiguration(
          new EndpointConfiguration("http://localhost:4568",
                                    Regions.AP_NORTHEAST_1.getName))
        .build()
  }

}
