package company

import akka.NotUsed
import akka.stream.scaladsl.Flow
import domain.company.{Company, CompanyId, CompanyName}
import spray.json.{DefaultJsonProtocol, JsValue, JsonParser, RootJsonReader}

import scala.util.Try

trait ParseFlow extends GraphType {
  import ApplicationErrorConverters._

  case class DataJson(id: String, name: String)

  object CompanyJsonProtocol extends DefaultJsonProtocol {
    implicit val DataJsonFormat = jsonFormat(
      DataJson,
      "id",
      "name"
    )
  }

  protected val ParseMessageFlow: Flow[SourceO, ParseO, NotUsed] = {
    import CompanyJsonProtocol._
    Flow[SourceO] map { message =>
      Try(JsonParser(message.body).convertTo[DataJson] match {
        case json => Company(CompanyId(json.id), CompanyName(json.name))
      }).toParseError
    }
  }
}
