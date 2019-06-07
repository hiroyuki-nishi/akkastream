import java.util.UUID

import akka.NotUsed
import akka.stream.{ActorAttributes, Attributes}
import akka.stream.scaladsl.{Flow, Source}
import domain.Partition

trait PartitionFlow2 extends LambdaFlow2 {
  private val parallelism = 100

  def partitionFlow: Flow[Record, Unit, NotUsed] =
    Flow[Record]
      .log(name = "START Flow")
      .addAttributes(Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                                          onFinish = Attributes.LogLevels.Off))
      .flatMapConcat { x =>
        println("START record")
        val partitions = Seq.fill(parallelism)(Partition.create).toList
        Source(partitions)
          .groupBy(parallelism, a => Math.abs(a.##) % parallelism)
          .via(invokeLambdasFlow)
          .log(name = "partitionFlow")
          .addAttributes(
            Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                                 onFinish = Attributes.LogLevels.Off))
          .addAttributes(ActorAttributes.dispatcher(
            "blocking-io-dispatcher-partitionkey-status"))
          .mergeSubstreams
          .map(y => ())
      }
      .log(name = "END Flow")
      .addAttributes(Attributes.logLevels(onElement = Attributes.LogLevels.Info,
                                          onFinish = Attributes.LogLevels.Off))
}
