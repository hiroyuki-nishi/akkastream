package company

import software.amazon.awssdk.services.sqs.model.Message

import domain.company.Company

trait GraphType {
  type SourceO = Message

  type ParseI = SourceO
  type ParseO = Either[ApplicationState, Company]

  type DynamoDBI = ParseO
  type DynamoDBO = DynamoDBI
}
