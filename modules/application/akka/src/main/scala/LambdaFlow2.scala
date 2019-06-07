import java.util.UUID

import akka.NotUsed
import akka.stream.{ActorAttributes, Attributes}
import akka.stream.scaladsl.{Flow, RestartSource, Source}
import domain.{Lambda, Partition}

import scala.concurrent.duration
import scala.concurrent.duration.FiniteDuration

trait LambdaFlow2 {
  private val parallelism = 8
  private val minBackoff: FiniteDuration = duration.DurationInt(3).seconds
  private val maxBackoff: FiniteDuration = duration.DurationInt(96).seconds
  private val randomFactor: Double = 0.2
  private val maxRestarts: Int = 6

  private def invoke: Flow[Lambda, Lambda, NotUsed] =
    Flow[Lambda]
      .flatMapConcat { lambda =>
        RestartSource.onFailuresWithBackoff(
          minBackoff = minBackoff,
          maxBackoff = maxBackoff,
          randomFactor = randomFactor,
          maxRestarts = maxRestarts
        ) { () =>
          Source
            .single(lambda)
            .map { lambda =>
              Thread.sleep((new scala.util.Random).nextInt(30) * 1000 + 100)
              println(lambda.uuid)
              lambda
            }
            .log(name = "invoke")
            .addAttributes(
              Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                                   onFinish = Attributes.LogLevels.Off))
        }
      }

  def invokeLambdasFlow: Flow[Partition, Partition, NotUsed] =
    Flow[Partition]
      .flatMapConcat { partition =>
        println(s"START partition: ${partition.uuid}")
        val lambdas = Seq.fill(parallelism)(Lambda(partition.uuid)).toList
        Source(lambdas)
          .groupBy(parallelism, a => Math.abs(a.##) % parallelism)
          .via(invoke)
          .log(name = "invokeLambdasFlow")
          .addAttributes(
            Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                                 onFinish = Attributes.LogLevels.Off))
          .addAttributes(ActorAttributes.dispatcher(
            "blocking-io-dispatcher-invoke-lambda2"))
          .mergeSubstreams
          .map(y => {
            partition
          })
      }
      .map(p => {
        println(s"END partition: ${p.uuid}")
        p
      })
}
