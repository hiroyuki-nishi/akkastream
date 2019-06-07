package dynamodb

import com.amazonaws.regions.{Region, Regions}
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient
import com.amazonaws.services.dynamodbv2.document._
import com.amazonaws.services.dynamodbv2.document.spec.ScanSpec

import scala.util.Try

trait DynamoDBWrapper {
  protected val tableName: String
  protected val regionName: String

  lazy val dynamoDBClient =
    AmazonDynamoDBClient.builder().withRegion(regionName).build()

  lazy private val dynamoDB = new DynamoDB(dynamoDBClient)

  protected def getTable(name: String = tableName) = Try(
    dynamoDB.getTable(name)
  )

  protected def getItem[E](hashKeyName: String, hashKeyValue: String)(
      record2Entity: Item => Try[E]): Try[E] =
    for {
      t <- getTable()
      i <- Try(t.getItem(hashKeyName, hashKeyValue))
      e <- record2Entity(i)
    } yield e

  protected def scan(scanSpec: ScanSpec): Try[ItemCollection[ScanOutcome]] =
    for {
      t <- getTable()
      items <- Try(t.scan(scanSpec))
    } yield items

  protected def scanItem[E](scanSpec: ScanSpec)(
      record2Entity: ItemCollection[ScanOutcome] => Try[E]): Try[E] =
    for {
      t <- getTable()
      i <- Try(t.scan(scanSpec))
      e <- record2Entity(i)
    } yield e

  protected def put(item: Item): Try[PutItemOutcome] =
    for {
      t <- getTable()
      r <- Try(t.putItem(item))
    } yield r
}
