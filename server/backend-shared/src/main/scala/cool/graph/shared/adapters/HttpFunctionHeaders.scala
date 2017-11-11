package cool.graph.shared.adapters

import cool.graph.shared.errors.SystemErrors

object HttpFunctionHeaders {
  import cool.graph.util.json.Json._
  import spray.json.DefaultJsonProtocol.StringJsonFormat
  import spray.json._

  implicit val seqToJsObjectFormatter = new JsonFormat[Seq[(String, String)]] {
    override def write(seq: Seq[(String, String)]): JsValue = {
      val fields = seq.map {
        case (key, value) => (key, JsString(value))
      }
      JsObject(fields: _*)
    }

    override def read(json: JsValue): Seq[(String, String)] = {
      json.asJsObject.fields.map {
        case (key, jsValue) => key -> jsValue.convertTo[String]
      }.toSeq
    }
  }

  def read(headersJson: Option[String]): Seq[(String, String)] = {
    val json = headersJson.getOrElse("{}")
    json.tryParseJson.getOrElse(throw SystemErrors.InvalidFunctionHeader(json)).convertTo[Seq[(String, String)]]
  }

  def readOpt(headersJson: Option[String]): Option[Seq[(String, String)]] = {
    headersJson.map(_.parseJson.convertTo[Seq[(String, String)]])
  }

  def write(headers: Seq[(String, String)]): JsObject = {
    headers.toJson.asJsObject
  }
}
