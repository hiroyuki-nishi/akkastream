import akka.actor.{ActorSystem, Cancellable}
import akka.actor.Status.Success
import akka.event.{Logging, LoggingAdapter}
import akka.stream.scaladsl._
import akka.stream.{
  ActorAttributes,
  ActorMaterializer,
  ActorMaterializerSettings,
  Attributes,
  Supervision,
  ThrottleMode
}
import akka.{Done, NotUsed}
import org.slf4j.LoggerFactory

import scala.concurrent.{ExecutionContext, Future, duration}
import scala.util.Random
import scala.concurrent.duration._

case class Record()

object Main extends App with PartitionFlow {
  implicit val system = ActorSystem("ActorSystem")
//  implicit val materializer = ActorMaterializer()
//  implicit val executionContext = system.dispatcher
  val dispatcherName = "blocking-io-dispatcher-invoke-lambda1024"

  lazy val source: Source[Record, NotUsed] = Source
    .repeat(Record())

  source.via(partitionFlow).runWith(Sink.ignore)
}
