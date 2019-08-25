import akka.{Done, NotUsed}
import akka.actor.ActorSystem
import akka.stream.{
  ActorMaterializer,
  OverflowStrategy,
  QueueOfferResult,
  ThrottleMode
}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import org.scalatest.FunSuite

import scala.concurrent.duration._
import scala.concurrent.{Future, duration}

class QueueFlowTest extends FunSuite {

  trait Fixture {
    implicit val system = ActorSystem("ActorSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
  }

  ignore("Source queue") {
    new Fixture {
      val bufferSize = 10
      val elementsToPrcess = 5
      val queue = Source
        .queue[Int](bufferSize, OverflowStrategy.backpressure)
        .throttle(elementsToPrcess, duration.DurationInt(3).second)
        .map(x => x * x)
        .toMat(Sink.foreach(x => println(s"completed $x")))(Keep.left)
        .run()

      val source = Source(1 to 10)

      implicit val ec = system.dispatcher
      source
        .mapAsync(1)(x => {
          queue.offer(x).map {
            case QueueOfferResult.Enqueued => println(s"enqueued $x")
            case QueueOfferResult.Dropped  => println(s"dropped $x")
            case QueueOfferResult.Failure(ex) =>
              println(s"Offer failed ${ex.getMessage}")
            case QueueOfferResult.QueueClosed => println("Sourcer Queue closed")
          }
        })
        .runWith(Sink.ignore)
    }
  }

  test("repeat") {
    new Fixture {
//        Source(1 to 10).map(_ * 2).runForeach(println)
      val source: Source[Int, NotUsed] = Source(1 to 100)
//      val flow1 = Flow[Int].throttle(1, 1.second, 0, ThrottleMode.shaping)
//      val flow2: source.Repr[Int] = source.throttle(1, 1.second, 0, ThrottleMode.shaping)
      val flow3: source.Repr[Int] = source.buffer(1, OverflowStrategy.dropHead)
      flow3.runForeach(println)
    }
  }
}
