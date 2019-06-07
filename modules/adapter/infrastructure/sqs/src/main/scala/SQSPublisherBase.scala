import spray.json.JsObject

import scala.util.Try

trait SQSPublisherBase {
  private def generateJson(dataJson: JsObject): Try[JsObject] = Try {
    JsObject("data" -> dataJson)
  }
}
