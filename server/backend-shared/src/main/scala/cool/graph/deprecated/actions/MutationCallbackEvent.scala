package cool.graph.deprecated.actions

import cool.graph.deprecated.actions.EventJsonProtocol.jsonFormat4
import spray.json.{DefaultJsonProtocol, JsObject}

case class MutationCallbackEvent(id: String, url: String, payload: String, headers: JsObject = JsObject.empty)

object EventJsonProtocol extends DefaultJsonProtocol {
  implicit val mutationCallbackEventFormat = jsonFormat4(MutationCallbackEvent)
}
