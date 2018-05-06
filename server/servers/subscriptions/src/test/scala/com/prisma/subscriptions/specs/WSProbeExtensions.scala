package com.prisma.subscriptions.specs

import akka.http.scaladsl.model.ws.TextMessage
import akka.http.scaladsl.testkit.WSProbe

object WSProbeExtensions {
  implicit class WSProbeExtensions(wsProbe: WSProbe) {
    def expectMessageContains(text: String): Unit = wsProbe.expectMessage() match {
      case t: TextMessage ⇒
        val message = t.getStrictText
        assert(message.contains(text), s"""Expected Message to include $text but got $message""")
      case _ ⇒
        throw new AssertionError(s"""Expected TextMessage("$text") but got BinaryMessage""")
    }
  }
}
