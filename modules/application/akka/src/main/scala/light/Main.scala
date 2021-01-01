package light

import akka.{Done, NotUsed}
import akka.actor.{ActorSystem, Cancellable}
import akka.stream.ActorAttributes.Dispatcher
import akka.stream.{ActorAttributes, ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt

object Main extends App {
  implicit val system = ActorSystem("ActorSystem")
  // default strategy is to stop all processing of the stream
  // default strategy can be overridden on the materializer
  private val decider: Supervision.Decider = {
    /*
     Stop: Stream is terminated with an error.
     Resume: Failing element is dropped and the stream continues.
     Restart: The element is dropped and the stream continues after
       restarting the stage. Any state accumulated by that stage will
       be cleared.
     */
    //    case _: MyException => Supervision.Resume
    case _ => Supervision.Stop
  }
  implicit val materializer = ActorMaterializer(
    ActorMaterializerSettings(system)
      .withSupervisionStrategy(decider)
  )

  def runMat = Source(1 to 10)
    .via(Flow[Int].map(_ * 2))
    .to(Sink.foreach[Int](println))
    .run()

  def runMaterialized() = {
    val source = Source.tick(1.second, 1.second, "Hello World")
    val sink = Sink.foreach[Any](println)
    val foldSink: Sink[Int, Future[String]] = Sink.fold("") {
      _ + _.toString
    }
    val flow = Flow[String].map(_.split(" "))

    val matSource: Cancellable = source.to(sink).run()
    val matFlow: NotUsed = source.viaMat(flow)(Keep.right).to(sink).run()
    val matSink: Future[Done] = source.toMat(sink)(Keep.right).run()
    implicit val executionContext = system.dispatcher
    // Sinkの集計結果なのでFuture[String]になる
    //    Source(1 to 10).runWith(foldSink).onComplete(x => {
    Source(1 to 10).toMat(foldSink)(Keep.right).run.onComplete(x => {
      println(x)
    })
  }

  private def runShortcuts(): Unit = {
    // runWith: attaches Sink and run
    Source(1 to 10).runWith(Sink.foreach(println))
    // creates and runs a Sink.foreach
    Source(1 to 10).runForeach(println)
    // runs a Sink.fold
    implicit val executionContext = system.dispatcher
    Source(1 to 10).runFold(0)(_ + _).onComplete(x => {
      println(x)
    })
    // runs a Sink.reduce
    Source(1 to 10).runReduce(_ + _).onComplete(x => {
      println(x)
    })
  }

  /*
    Stage毎にAttribute(log, dispatcher, supervision...etc)を設定できる。
    このサンプルではapplication.confに設定されているdispatcher設定で動くサンプル。
   */
  private def runAttributes(): Unit = {
    // reference resources/application.conf
    Source(1 to 10)
      .withAttributes(ActorAttributes.dispatcher("myDispatcher"))
      .runForeach(println)
  }

  /*
    Stage毎にSupervisionを設定できる。
    このサンプルではArithmeticExceptionが発生しても
    処理を続けるサンプル。
   */
  private def runCustomSupervision(): Unit = {
    val decider: Supervision.Decider = {
      case _: ArithmeticException =>
        println("ArithmeticException")
        Supervision.Resume
      case _ => Supervision.Stop
    }
    val possibleDivisionByZero =
      Flow[Int].map(i => 100 / i)
        .withAttributes(
          ActorAttributes.supervisionStrategy(decider)
        )
    Source(0 to 10).via(possibleDivisionByZero).runForeach(println)
  }

  /*
    RecoverするFlow
    recoverされるとstreamがストップする
    terminate a stream on failure with a specific value
   */
  def runTerminateBySpecificValue() = {
    val recoverWithZero: Flow[Int, Int, NotUsed] = {
      Flow[Int].map(10 / _).recover {
        case _: ArithmeticException => 0
        case _ => throw new RuntimeException("ERROR")
      }
    }
    implicit val executionContext = system.dispatcher
    Source(0 to 10).via(recoverWithZero).runForeach(println).onComplete { x =>
      println(x)
    }
  }

  //  runShortcuts()
  //  runMaterialized
  //  runMat

  //    runAttributes()
  runCustomSupervision
//  runRecoverFlow()
}
