package company

import akka.event.{Logging, LoggingAdapter}
import akka.stream._
import akka.stream.alpakka.sqs.scaladsl.SqsSource
import akka.stream.scaladsl.{
  Broadcast,
  Flow,
  GraphDSL,
  RunnableGraph,
  Sink,
  Zip
}
import akka.{Done, NotUsed}
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClient}
import software.amazon.awssdk.services.sqs.model.Message
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.Future

trait MainFlow extends GraphType with ParseFlow with DynamoDBFlow {
  protected def mainFlow(queueUrl: String)(
      implicit sqsClient: SqsAsyncClient): RunnableGraph[Future[Done]] =
    RunnableGraph.fromGraph(GraphDSL.create(Sink.ignore) { implicit b => sink =>
      import GraphDSL.Implicits._

      val source: SourceShape[SourceO] =
        b.add(SqsSource(queueUrl))

      val parseFlow: FlowShape[ParseI, ParseO] =
        b.add(ParseMessageFlow
          .log("parse") //TODO -nishi ログの調査
          .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel)))

      val dynamoDBFlow: FlowShape[DynamoDBI, DynamoDBO] =
        b.add(DynamoDBFlow
          .log("dynamodb")
          .withAttributes(Attributes.logLevels(onElement = Logging.InfoLevel)))

      source ~> parseFlow ~> dynamoDBFlow ~> sink
      ClosedShape
    })
}
