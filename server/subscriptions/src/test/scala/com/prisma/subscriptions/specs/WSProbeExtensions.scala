package cool.graph.subscriptions.specs

import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.testkit.WSProbe
import spray.json.JsValue
import cool.graph.util.json.Json._

object WSProbeExtensions {
  implicit class WSProbeExtensions(wsProbe: WSProbe) {
    def expectMessageContains(text: String): Unit = wsProbe.expectMessage() match {
      case t: TextMessage ⇒
        val message = t.getStrictText
        assert(message.contains(text), s"""Expected Message to include $text but got $message""")
      case _ ⇒
        throw new AssertionError(s"""Expected TextMessage("$text") but got BinaryMessage""")
    }

    def expectJsonMessage(): JsValue = {
      val msg = wsProbe.expectMessage().asTextMessage.getStrictText
      msg.tryParseJson.get
    }
  }
}
