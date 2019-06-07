package company

import akka.actor.{ActorSystem, Props}
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import com.amazonaws.services.sqs.{AmazonSQSAsync, AmazonSQSAsyncClient}
import domain.RepositoryError
import domain.company.{Company, CompanyId, CompanyRepository}
import dynamodb.company.CompanyRepositoryOnDynamoDB
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.sqs.SqsAsyncClient

import scala.concurrent.ExecutionContext
import scala.util.Try

object Main extends App with MainFlow {
  implicit val sqsClient: SqsAsyncClient = SqsAsyncClient
    .builder()
    .region(Region.AP_NORTHEAST_1)
    .build()
  implicit val system: ActorSystem = ActorSystem("mySystem")
  implicit val executionContext: ExecutionContext = system.dispatcher
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val adapter: LoggingAdapter = Logging(system, this.getClass)
  override val companyRepository = new CompanyRepository
  with CompanyRepositoryOnDynamoDB
  mainFlow("").run().onComplete { done =>
    done.fold(
      t => adapter.error(t, "error"),
      _ => ()
    )
    materializer.shutdown()
    system.terminate().onComplete { _ =>
      adapter.info("terminated")
      sys.exit()
    }
  }
}
