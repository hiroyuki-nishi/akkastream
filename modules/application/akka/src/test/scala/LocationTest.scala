package target

import java.nio.charset.Charset

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ActorAttributes, ActorMaterializer, Attributes, ClosedShape}
import akka.stream.scaladsl.{Flow, GraphDSL, RunnableGraph, Sink, Source}
import com.amazonaws.auth.STSAssumeRoleSessionCredentialsProvider
import com.amazonaws.auth.profile.ProfileCredentialsProvider
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3ClientBuilder
import com.amazonaws.services.s3.model.S3ObjectInputStream
import company.ApplicationState
import domain.Partition
import domain.company.{Company, CompanyId, CompanyName}
import kantan.csv._
import org.scalatest.{BeforeAndAfterAll, DiagrammedAssertions, WordSpec}

import scala.collection.immutable.HashMap
import scala.concurrent.{ExecutionContextExecutor, Future}
import scala.util.{Failure, Success, Try}

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

final case class Log4(companyId: String, deviceId: String)


class LocationTest extends WordSpec with BeforeAndAfterAll with DiagrammedAssertions {
  type CompanyId = String
  type DeviceId = String

  implicit val logDecoder: RowDecoder[RawLog] = RowDecoder.decoder(0, 1, 2, 3, 4, 5, 6, 7)(RawLog.apply)
  implicit val logDecoder2: RowDecoder[Log4] = RowDecoder.decoder(0, 1)(Log4.apply)

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

  trait WithFixture extends AmazonS3BucketWrapper {
    override val bucketName: CompanyId = "aws-athena-query-results-358925038325-ap-northeast-1"
    implicit val system: ActorSystem = ActorSystem("ActorSystem")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val executionContext: ExecutionContextExecutor = system.dispatcher
    val dummyDevicesMap =
      HashMap(
        "9512192a18d42848eef03f3f0a3dfbf6" -> Device("9512192a18d42848eef03f3f0a3dfbf6", "Dummyデバイス1", 999, "内田健太"),
        "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA" -> Device("DDDDDDDDDDDDDDDDDDDDDDDDDDDDDDDD", "Dummyデバイス2", 1000, "爽やか三組")
      )
  }

  //  def testCsvReader = {
  //    val log =
  //      """hoge, fuga
  //        |AAAA, BBBB
  //        |""".stripMargin
  //    val reader = log.asCsvReader[Log4](rfc)
  //    println(reader.foreach(println))
  //  }

  "S3にcsvファイルがある" when {
    "AkkaStreamでS3のcsvファイルを取得後、変換してローカルにout.csvとして出力する" should {
      "out.csvが出力される" in new WithFixture {
        val dispatcherName = "blocking-io-dispatcher-invoke-lambda128"
        //        def createEmptyCSVFile(name: String, encode: Charset, lineFeedCode: LineFeedCode): Try[SFile] = Try {
        //          val fileName = s"$name.csv"
        //          val outputFile = SFile(fileName)
        //          val bow = outputFile.newBufferedWriter(charset = encode)
        //          val writer = newCSVWriter(bow, lineEnd(lineFeedCode))
        //          writer.writeNext(HEADER)
        //          writer.close()
        //          System.gc()
        //
        //          logger.info(s"[ID: $trace_id] Success to create empty csv file.")
        //          outputFile
        //        }

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
              case Success(s) => {
                //                println(s)
                s
              }
              case Failure(e) => throw new RuntimeException(e)
            }
          }

        def output(path: String): Unit = get(path).fold(
          e => throw new RuntimeException(e),
          s => {
            val stream = scala.io.Source.fromInputStream(s, "UTF-8")
            Source.fromIterator(() => stream.getLines())
              // TODO -nishi mapAsync(2)とかgroupByにするとログ順がバラバラになると思う
              .via(convertLogFlow)
              .log(name = "logFlow")
              .addAttributes(
                Attributes.logLevels(onElement = Attributes.LogLevels.Info,
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
          }
        )

        def createZipFlow: Flow[S3ObjectInputStream, Log, NotUsed] =
          Flow[S3ObjectInputStream].flatMapConcat { s3 =>
            val stream = scala.io.Source.fromInputStream(s3, "UTF-8")
            Source.fromIterator(() => stream.getLines())
              .via(convertLogFlow)
              .log(name = "convertLogFlow")
              .addAttributes(
                Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                  onFinish = Attributes.LogLevels.Off))
            // TODO -nishi Zipファイル作成
          }

        val getS3Flow: Flow[String, Any, NotUsed] =
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
              .via(createZipFlow)
              .log(name = "createZipFlow")
              .addAttributes(
                Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                  onFinish = Attributes.LogLevels.Off))
          }.map(path => "")

        def execute(executions: List[String]) = {
          Source(executions).zipWithIndex
            .groupBy(executions.size, a => a._2 % executions.size)
            .map(_._1)
            .via(getS3Flow)
            .mergeSubstreams
            .log(name = "END Flow")
            .addAttributes(Attributes.logLevels(onElement = Attributes.LogLevels.Info,
              onFinish = Attributes.LogLevels.Off))
            .runWith(Sink.ignore)
        }

        // TODO -nishi GraphDSLの流れ
        // 実行ID一覧を取得() ~> デバイス情報の取得() ~> S3からcsv取得
        // ~> Logに変換 ~> Readme作成() ~> Zipファイル作成()

        //        execute(Seq("hoge1.csv").toList)
        //          .onComplete { done =>
        //          done.fold(
        //            t => println(t, "error"),
        //            _ => ()
        //          )
        //          // materializer.shutdown()
        //          system.terminate().onComplete { _ =>
        //            println("terminated")
        //            sys.exit()
        //          }
        //        }
      }
    }
  }

  "xxxxx" when {
    "yyyyyyy" should {
      "zzzz" in new WithFixture {
        RunnableGraph
          .fromGraph(GraphDSL.create(Sink.ignore) { implicit builder => sink =>
            import akka.stream.scaladsl.GraphDSL.Implicits._
            val source: Source[String, NotUsed] =
              Source.single("hoge1.csv")

            val s3Flow: Flow[String, String, NotUsed] =
              Flow[String].map { x =>
                println(x)
                Thread.sleep(1000)
                x
              }

            source ~> s3Flow ~> sink
            ClosedShape
          })
          .run()
          .foreach(print)
      }
    }
  }
}
