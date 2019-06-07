package dynamodb.company

import com.amazonaws.regions.Regions
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec
import com.amazonaws.services.dynamodbv2.document.{
  Item,
  ItemCollection,
  ScanOutcome
}
import domain.{RepositoryError, RepositoryNotFoundError}
import domain.company.{
  Company,
  CompanyId,
  CompanyName,
  CurrentContractGetOutput,
  DomainDate,
  Subdomain
}
import dynamodb.DynamoDBWrapper

import scala.collection.JavaConverters._
import scala.util.Try

trait CompanyRepositoryOnDynamoDB extends DynamoDBWrapper {

  import dynamodb.Converter._
  val regionName: String =
    sys.env.getOrElse("region", Regions.AP_NORTHEAST_1.getName)
  override lazy val tableName: String = "company"
  val AttrId = "id"
  val AttrName = "name"

  private def item2Company(item: Item): Company = {
    Company(
      id = CompanyId(item.getString(AttrId)),
      name = CompanyName(item.get(AttrName).toString)
    )
  }

  private def record2Entity(item: Item): Try[Option[Company]] = Try {
    Option(item2Company(item))
  }

  private def record2Entity(
      items: ItemCollection[ScanOutcome]): Try[Option[Set[Company]]] = Try {
    for {
      set <- Option(items.iterator().asScala.toSet)
      contracts <- Option(set.map(item2Company))
    } yield contracts
  }

  def findBy(id: CompanyId): Either[RepositoryError, Option[Company]] =
    getItem(AttrId, id.value)(record2Entity).toRepositoryError

  def get(id: CompanyId): Either[RepositoryError, Company] =
    findBy(id).flatMap {
      case Some(c) => Right(c)
      case None    => Left(RepositoryNotFoundError())
    }

  def findAll(): Either[RepositoryError, Option[Set[Company]]] =
    scanItem(new ScanSpec())(record2Entity).toRepositoryError

  def gets(): Either[RepositoryError, Set[Company]] =
    findAll().flatMap {
      case Some(c) => Right(c)
      case None    => Left(RepositoryNotFoundError())
    }

  private def generateItem(company: Company): Item =
    new Item()
      .withString(AttrId, company.id.value)
      .withString(AttrName, company.name.value)

  def insertInternal(company: Company): Either[RepositoryError, Company] = {
    val item = generateItem(company)
    put(item).map(_ => company).toRepositoryError
  }
}
