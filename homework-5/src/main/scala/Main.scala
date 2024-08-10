import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source, ZipWith}
import akka.stream.{ClosedShape, Graph}

object Main {
  implicit val system: ActorSystem = ActorSystem("homework-5")

  private val graph: Graph[ClosedShape.type, NotUsed] = GraphDSL.create() { implicit builder =>
    import GraphDSL.Implicits._

    val input = builder.add(Source(1 to 5))
    val multiplyByTen = builder.add(Flow[Int].map(x => x * 10))
    val multiplyByTwo = builder.add(Flow[Int].map(x => x * 2))
    val multiplyByThree = builder.add(Flow[Int].map(x => x * 3))
    val outputStream = builder.add(Sink.foreach(println))
    val broadcast = builder.add(Broadcast[Int](3))
    val zipStream = builder.add(ZipWith[Int, Int, Int, (Int, Int, Int)]((a, b, c) => (a, b, c)))

    input ~> broadcast
    broadcast.out(0) ~> multiplyByTen ~> zipStream.in0
    broadcast.out(1) ~> multiplyByTwo ~> zipStream.in1
    broadcast.out(2) ~> multiplyByThree ~> zipStream.in2
    zipStream.out ~> outputStream

    ClosedShape
  }

  def main(args: Array[String]): Unit = {
    RunnableGraph.fromGraph(graph).run()
  }
}