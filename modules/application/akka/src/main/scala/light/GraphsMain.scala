package light

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.{ClosedShape, SourceShape, UniformFanInShape}
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source}

object GraphsMain extends App {
  implicit val system = ActorSystem("ActorSystem")

  /*
  [Fan Out] Junctions
  * Broadcast[T] (1 input N outputs) Incoming elements are emitted all outputs.
  * Balance[T] (1 input N outputs) Incoming elements are emitted to one of the outputs(first available)
  * UnzipWith[In, A, B, ...] (1 input, N outputs) Uses a function to convert
      1 input element into N outputs elements and emits one to each output.
  * UnZip[A,B] (1 input, 2 outputs) Splits a stream of Tuple2[A,B] into
      two streams of A and B.

  [Fan In] Junctions
  https://doc.akka.io/docs/akka/current/stream/operators/Source/combine.html
  * Merge[In] (N inputs, 1 output) randomly selects from inputs push to a single output.
  * MergePreferred[In] (N inputs, 1 output) similar to Merge, but one output
      is given higher priority over all others.
  * ZipWith[A, B, ..., Out] (N inputs, 1 output) Uses a function to take one
     element from each input and convert them all int a single output..
  * Zip[A, B] (2 inputs, 1 output) Zips two streams of A and B into a single
     stream of Tuple2[A, B]
  * Concat[A] (2 inputs, 1 output) concatenates two streams. Consumes one
     completely before the other.
  */

  /* Closed Graphs
    input と outputがOpenされていないGraph.
    これはGraphが実行可能なことを意味している。
   */
  private val closedGraph = RunnableGraph.fromGraph(GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._
      val source = Source(1 to 10)
      val sink = Sink.foreach(println)
      val broadcast = builder.add(Broadcast[Int](2))
      val merge = builder.add(Merge[Int](2))
      val f1, f2, f3, f4 = Flow[Int].map(_ + 10)

      source ~> f1 ~> broadcast ~> f2 ~> merge ~> f4 ~> sink
                      broadcast ~> f3 ~> merge
      ClosedShape
  }).run()

  /*
   Partial Graphs(SourceShape)
   SourceのGraph
   */
  private val source: Source[Int, NotUsed] = Source.fromGraph(
    GraphDSL.create() {
      implicit builder: GraphDSL.Builder[NotUsed] =>
        import GraphDSL.Implicits._
        val source1, source2 = Source(1 to 10)
        val merge = builder.add(Merge[Int](2))

        source ~> merge
        source ~> merge

        SourceShape(merge.out)
    }
  )

  /*
    FanIn, FanOutのGraph
   */
  private val dualPrtFainIn = GraphDSL.create() {
    implicit builder: GraphDSL.Builder[NotUsed] =>
      val merge = builder.add(Merge[Int](2))
      UniformFanInShape[Int, Int](
        merge.out,
        merge.in(0),
        merge.in(1)
      )
  }
}
