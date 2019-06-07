import akka.actor.{ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object MainServer extends App {
  implicit val system = ActorSystem("lspan-system")
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val route =
    path("hello") {
      get {
        complete(
          HttpEntity(ContentTypes.`text/html(UTF-8)`, "<h1>Hello World!</h1>"))
      }
    }
  val bindingFuture = Http().bindAndHandle(route, "0.0.0.0", 8080)
//  bindingFuture
//    .flatMap(_.unbind())
//    .onComplete(_ => {
//      materializer.shutdown()
//      system.terminate().onComplete{_ =>
//        println("terminated")
//        sys.exit()
//      }
//    })
}
