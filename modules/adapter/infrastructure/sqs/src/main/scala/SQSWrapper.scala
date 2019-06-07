import com.amazonaws.services.sqs.{AmazonSQS, AmazonSQSClient}
import com.amazonaws.services.sqs.model._

import scala.collection.JavaConverters._
import scala.util.Try

trait SQSWrapper {
  protected val regionName: String
  protected val queueName: String
  protected val LimitSize = 200

  lazy protected val sqsClient: AmazonSQS =
    AmazonSQSClient.builder().withRegion(regionName).build()

  protected lazy val queueUrl = Try {
    sqsClient.getQueueUrl(queueName)
  }

  def listQueue: Try[ListQueuesResult] = Try {
    sqsClient.listQueues()
  }

  def approximateNumberOfMessages: Try[Int] = {
    import QueueAttributeName._
    for {
      url <- queueUrl
      approximateNumberOfMessages <- Try {
        val request = new GetQueueAttributesRequest()
          .withQueueUrl(url.getQueueUrl)
          .withAttributeNames(
            ApproximateNumberOfMessages
          )
        val res = sqsClient.getQueueAttributes(request)
        res.getAttributes
          .getOrDefault(ApproximateNumberOfMessages.toString, "0")
          .toInt
      }
    } yield approximateNumberOfMessages
  }

  def receiveMessage: Try[Seq[Message]] =
    for {
      url <- queueUrl
      message <- Try {
        sqsClient.receiveMessage(
          new ReceiveMessageRequest()
            .withQueueUrl(url.getQueueUrl)
            .withMaxNumberOfMessages(10)
        )
      }
    } yield message.getMessages.asScala

  def approximateReceiveMessages: Try[Seq[Message]] = {

    def recursiveMessages(
        limit: Int,
        messages: Seq[Message] = Seq.empty): Try[Seq[Message]] = {
      receiveMessage flatMap { ms =>
        val res = messages ++ ms
        if (ms.nonEmpty && res.size < limit) {
          recursiveMessages(limit, res)
        } else {
          Try(res)
        }
      }
    }

    for {
      count <- approximateNumberOfMessages
      messages <- recursiveMessages(if (LimitSize < count) LimitSize else count)
    } yield messages
  }

  private def purgeQueue =
    for {
      url <- queueUrl
      _ <- Try {
        val purge = new PurgeQueueRequest()
          .withQueueUrl(url.getQueueUrl)
        sqsClient.purgeQueue(purge)
      }
    } yield ()

  private def sendMessage(queueUrl: String, messageBody: String) = Try {
    sqsClient.sendMessage(queueUrl, messageBody)
  }

  def sendMessage(messageBody: String): Try[SendMessageResult] =
    for {
      url <- queueUrl
      res <- sendMessage(url.getQueueUrl, messageBody)
    } yield res

  private def deleteMessage(queueUrl: String, receiptHandle: String) = Try {
    sqsClient.deleteMessage(queueUrl, receiptHandle)
  }

  def deleteMessage(receiptHandle: String): Try[Unit] =
    for {
      url <- queueUrl
      _ <- deleteMessage(url.getQueueUrl, receiptHandle)
    } yield ()

  private def sendFIFOMessage(queueUrl: String,
                              messageBody: String,
                              messageGroupId: String) = Try {
    val request = new SendMessageRequest()
      .withQueueUrl(queueUrl)
      .withMessageBody(messageBody)
      .withMessageGroupId(messageGroupId)
    sqsClient.sendMessage(request)
  }

  def sendFIFOMessage(messageBody: String,
                      messageGroupId: String): Try[SendMessageResult] =
    for {
      url <- queueUrl
      res <- sendFIFOMessage(url.getQueueUrl, messageBody, messageGroupId)
    } yield res
}
