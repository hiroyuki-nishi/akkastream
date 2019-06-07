import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ClosedShape}
import akka.stream.scaladsl.{GraphDSL, RunnableGraph, Sink, Source}
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.client.builder.AwsClientBuilder
import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder
import company.{DynamoDBFlow, GraphType}
import domain.company.{Company, CompanyId, CompanyName, CompanyRepository}
import dynamodb.company.CompanyRepositoryOnDynamoDB
import org.scalatest.FunSuite

class DynamoDBFlowTest extends FunSuite with GraphType with DynamoDBFlow {

  trait Fixture {
    implicit val system = ActorSystem("test-stream")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
  }

  val companyRepository: CompanyRepository = new CompanyRepository
  with CompanyRepositoryOnDynamoDB {
    override lazy val dynamoDBClient =
      AmazonDynamoDBClientBuilder
        .standard()
        .withCredentials(
          new AWSStaticCredentialsProvider(new BasicAWSCredentials("", "")))
        .withEndpointConfiguration(
          new AwsClientBuilder.EndpointConfiguration(
            "http://localhost:4569",
            Regions.AP_NORTHEAST_1.getName))
        .build()
  }

  test("dynamoDBFlowの動作確認") {
    new Fixture {
      RunnableGraph
        .fromGraph(GraphDSL.create(Sink.ignore) { implicit builder => sink =>
          import akka.stream.scaladsl.GraphDSL.Implicits._
          val source: Source[DynamoDBI, NotUsed] =
            Source.single(Right(Company(CompanyId("1"), CompanyName("hoge"))))
          companyRepository.insertInternal(
            Company(CompanyId("1"), CompanyName("hoge")))
          println(companyRepository.findAll())
          source ~> DynamoDBFlow ~> sink
          ClosedShape
        })
        .run()
        .foreach(print)
    }
  }
}
