import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, ActorMaterializer, Attributes}
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.S3ObjectInputStream

import scala.collection.immutable.HashMap
import scala.concurrent.ExecutionContextExecutor
import scala.util.{Failure, Success, Try}

trait AmazonS3BucketWrapper {
  protected val bucketName: String
  val client =
    AmazonS3ClientBuilder.standard()
      .withRegion(Regions.AP_NORTHEAST_1)
      .withCredentials(
        new STSAssumeRoleSessionCredentialsProvider(
          "arn:aws:iam::358925038325:role/SwitchRole",
          "white"
        )
      )
      .withCredentials(new ProfileCredentialsProvider("white"))
      .build()

  def get(key: String): Try[S3ObjectInputStream] = {
    for {
      s3obj <- Try(client.getObject(bucketName, key))
      stream <- Try(s3obj.getObjectContent)
    } yield stream
  }
}

final case class RawLog(companyId: String,
                        deviceId: String,
                        latitude: String,
                        longitude: String,
                        log_time: String,
                        appStatus: String,
                        address: String,
                        date: String)

final case class Log(companyId: String,
                     assetNo: Int,
                     userName: String,
                     name: String,
                     latitude: String,
                     longitude: String,
                     log_time: String,
                     address: String)

final case class Device(deviceId: String,
                        name: String,
                        assetNo: Int,
                        userName: String)

object LocationLogMain extends App with AmazonS3BucketWrapper {
  type CompanyId = String
  type DeviceId = String

  override val bucketName = "aws-athena-query-results-358925038325-ap-northeast-1"
  implicit val system: ActorSystem = ActorSystem("ActorSystem")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val executionContext: ExecutionContextExecutor = system.dispatcher
  val dummyDevicesMap =
    HashMap(
      "9512192a18d42848eef03f3f0a3dfbf6" -> Device("9512192a18d42848eef03f3f0a3dfbf6", "Dummyデバイス1", 999, "内田健太"),
      "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> Device("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD", "Dummyデバイス2", 1000, "爽やか三組")
    )

  def convertLog(log: RawLog, devices: HashMap[String, Device]): Log = {
    devices.get(log.deviceId) match {
      case None =>
        Log(log.companyId,
          0,
          "",
          "",
          log.latitude,
          log.longitude,
          log.log_time,
          log.address)
      case Some(d) =>
        Log(log.companyId,
          d.assetNo,
          d.userName,
          d.name,
          log.latitude,
          log.longitude,
          log.log_time,
          log.address)
    }
  }

  def convertLogFlow: Flow[String, Log, NotUsed] =
    Flow[String].map { l =>
      // TODO -nishi for式の途中で失敗したらAkkaStreamがどうなるか要検証
      (for {
        // TODO -nishi 微妙
        logString <- Try(l.split(","))
        rawLog <- Try(RawLog(
          logString(0),
          logString(1),
          logString(2),
          logString(3),
          logString(4),
          logString(5),
          logString(6),
          logString(7),
        ))
        log <- Try(convertLog(rawLog, dummyDevicesMap))
        // 作成ずみのSFileにlogを書き込んでいく
        // zip化
      } yield log) match {
        case Success(s) => s
        case Failure(e) => throw new RuntimeException(e)
      }
    }


  def writeCsvFlow: Flow[S3ObjectInputStream, Log, NotUsed] =
    Flow[S3ObjectInputStream].flatMapConcat { s3 =>
      val stream = scala.io.Source.fromInputStream(s3, "UTF-8")
      Source.fromIterator(() => stream.getLines())
        .via(convertLogFlow)
      //        .log(name = "convertLogFlow")
      //        .addAttributes(
      //          Attributes.logLevels(onElement = Attributes.LogLevels.Info,
      //            onFinish = Attributes.LogLevels.Off))
      // TODO -nishi Zipファイル作成
    }

  val getS3Flow: Flow[String, S3ObjectInputStream, NotUsed] =
    Flow[String].flatMapConcat { path =>
      println(s"START s3Flow object path: ${path}")
      Source.single(path)
        .map(p => {
          get(path)
            .fold(
              e => throw new RuntimeException(e),
              s => s
            )
        })
    }

  def executeFlow: Flow[List[String], Unit, NotUsed] =
    Flow[List[String]]
      .log(name = "START Flow")
      .addAttributes(Attributes.logLevels(onElement = Attributes.LogLevels.Info,
        onFinish = Attributes.LogLevels.Off))
      .flatMapConcat { executions =>
        Source(executions).zipWithIndex
          .groupBy(executions.size, a => a._2 % executions.size)
          .map(_._1)
          .via(getS3Flow)
          .via(writeCsvFlow)
          .map(x => println("実行"))
          .async
          .mergeSubstreams
          .addAttributes(ActorAttributes.dispatcher(
            "blocking-io-dispatcher-partitionkey-status"))
          .map(_ => ())
      }

  //  Source.single(Seq("hoge1.csv", "hoge2.csv").toList)
  Source.single(Seq("hoge1.csv").toList)
    //    execute(Seq("hoge1.csv", "hoge2.csv").toList)
    .via(executeFlow)
    .log(name = "END Flow")
    .addAttributes(Attributes.logLevels(onElement = Attributes.LogLevels.Info,
      onFinish = Attributes.LogLevels.Off))
    .runWith(Sink.ignore)
    .onComplete { done =>
      done.fold(
        t => println(t, "error"),
        _ => ()
      )
      // materializer.shutdown()
      system.terminate().onComplete { _ =>
        println("terminated")
        sys.exit()
      }
    }
  //  val hogeFlow: Flow[String, String, NotUsed] =
  //  Flow[String].map { path =>
  //    println("aaaaaa")
  //    path
  //  }
  //
  //  val fugaFlow: Flow[String, String, NotUsed] =
  //    Flow[String].map { path =>
  //      println("fuga")
  //      path
  //    }
  //
  //  RunnableGraph
  //    .fromGraph(GraphDSL.create(Sink.ignore) { implicit builder =>
  //      sink =>
  //        import akka.stream.scaladsl.GraphDSL.Implicits._
  //        val source: Source[String, NotUsed] =
  //          Source.single("hoge1.csv")
  //
  //        val broadcast = builder.add(Broadcast[String](2))
  //        val merge = builder.add(Merge[String](2))
  //
  //        source ~> broadcast ~> getS3Flow ~> writeCsvFlow ~> hogeFlow ~> merge ~> fugaFlow ~> sink
  //                  broadcast ~> merge
  //        ClosedShape
  //    })
  //    .run()
  //    .foreach(print)
}
