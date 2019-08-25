import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._
import akka.stream.{
  ActorAttributes,
  ActorMaterializer,
  ActorMaterializerSettings,
  Attributes,
  Supervision
}
import akka.{Done, NotUsed}
import org.scalatest.FunSuite

import scala.concurrent.Future
import scala.util.Random

class MainTest extends FunSuite {

  trait Fixture {
    implicit val system = ActorSystem("ActorSystem")
    implicit val materializer = ActorMaterializer()
    implicit val executionContext = system.dispatcher
  }

  ignore("deadlockの再現: 1Lambdaあたりの実行時間が1~30sでスレッドプールが128, groupKeyをindexで分割") {
    // 100 * 7 = 700並列で動くのでスレッドが枯渇しdeadlockになる
    new Fixture with PartitionFlow {
      val dispatcherName = "blocking-io-dispatcher-invoke-lambda128"
      lazy val source: Source[Record, NotUsed] = Source
        .repeat(Record())

      source.via(partitionFlow).runWith(Sink.ignore)
    }
  }

  ignore("deadlockの再現: 1Lambdaあたりの実行時間が1~30sでスレッドプールが12, groupKeyをハッシュ値で分割") {
    // スレッドプールを128の場合、つまりにくくなるが綺麗にグループ分割＋1Lambdaの実行時間がかかるとdeadlockしやすくなる
    new Fixture with PartitionFlow2 {
      lazy val source: Source[Record, NotUsed] = Source
        .repeat(Record())

      source.via(partitionFlow).runWith(Sink.ignore)
    }
  }

  ignore("deadlock修正案: 単純にスレッドプールのサイズを増やす") {
    // 全てのsubFlowを並行で動かす。partitionKey: max100 * lambda: max8 = 800は最低必要
    // 1024でランニング6hテストOK
    new Fixture with PartitionFlow {
      val dispatcherName = "blocking-io-dispatcher-invoke-lambda1024"
      lazy val source: Source[Record, NotUsed] = Source
        .repeat(Record())

      source.via(partitionFlow).runWith(Sink.ignore)
    }
  }

  test("mapAsync") {
    new Fixture {
      val dispatcherName = "blocking-io-dispatcher-invoke-lambda1024"
      lazy val source: Source[Record, NotUsed] = Source
        .repeat(Record())

      source
        .mapAsync(2)(r =>
          Future {
            Thread.sleep(1000)
            println("hoge")
            r
        })
        .addAttributes(
          Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                               onFinish = Attributes.LogLevels.Off))
        .addAttributes(ActorAttributes.dispatcher("mapasync-dispatcher"))
        .map(x => println(x))
        .take(10)
        .runForeach(println)
    }
  }
}
