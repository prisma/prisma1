package cool.graph.messagebus

import java.nio.charset.Charset

import play.api.libs.json._

/**
  * Common marshallers and unmarshallers
  */
object Conversions {
  type Converter[S, T] = S => T

  type ByteMarshaller[T]   = Converter[T, Array[Byte]]
  type ByteUnmarshaller[T] = Converter[Array[Byte], T]

  object Marshallers {
    val FromString: ByteMarshaller[String] = (msg: String) => msg.getBytes("utf-8")

    def FromJsonBackedType[T]()(implicit writes: Writes[T]): ByteMarshaller[T] = msg => {
      val jsonString = Json.toJson(msg).toString()
      FromString(jsonString)
    }
  }

  object Unmarshallers {
    val ToString: ByteUnmarshaller[String] = (bytes: Array[Byte]) => new String(bytes, Charset.forName("UTF-8"))

    def ToJsonBackedType[T]()(implicit reads: Reads[T]): ByteUnmarshaller[T] =
      msg => {
        Json.parse(msg).validate[T] match {
          case JsSuccess(v, _) => v
          case JsError(err)    => throw new Exception(s"Invalid json message format: $err")
        }
      }
  }
}
