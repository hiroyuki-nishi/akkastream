package company

import software.amazon.awssdk.services.sqs.model.Message
import akka.NotUsed
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL}
import domain.company.{Company, CompanyRepository}

trait DynamoDBFlow extends GraphType {
  import ApplicationErrorConverters._
  protected val companyRepository: CompanyRepository

  protected val DynamoDBFlow
    : Flow[DynamoDBI, Either[ApplicationState, Company], NotUsed] =
    Flow[DynamoDBI].map {
      _.flatMap {
        case c: Company =>
          for {
            c <- companyRepository.insertInternal(c).toApplicationError
          } yield {
            c
          }
      }
    }
}
