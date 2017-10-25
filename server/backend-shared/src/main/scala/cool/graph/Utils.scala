package cool.graph

import com.google.common.base.CaseFormat
import spray.json.{DefaultJsonProtocol, _}

object Utils {

  def camelToUpperUnderscore(str: String): String =
    CaseFormat.UPPER_CAMEL.to(CaseFormat.UPPER_UNDERSCORE, str)
}

case class Timing(name: String, duration: Long)
object TimingProtocol extends DefaultJsonProtocol {
  implicit val timingFormat: RootJsonFormat[Timing] = jsonFormat2(Timing)
}

object JsonFormats {

  implicit object CaseClassFormat extends JsonFormat[Product] {
    def write(x: Product): JsValue = {
      val values = x.productIterator.toList
      val fields = x.getClass.getDeclaredFields

      def getIdValue(p: Product): Option[Any] = {
        val values = p.productIterator.toList
        val fields = p.getClass.getDeclaredFields

        fields.zipWithIndex.find(_._1.getName == "id").map(z => values(z._2))
      }

      val map: Map[String, Any] = values.zipWithIndex.map {
        case (v, i) =>
          val key = fields(i).getName
          val value = v match {
            case v: Product if !v.isInstanceOf[Option[_]] =>
              getIdValue(v).getOrElse("...")
            case Some(v: Product) =>
              getIdValue(v).getOrElse("...")
            case v => v
          }

          key -> value
      }.toMap

      AnyJsonFormat.write(map)
    }

    def read(value: JsValue) = throw new UnsupportedOperationException()
  }

  implicit object AnyJsonFormat extends JsonFormat[Any] {
    def write(x: Any): JsValue = x match {
      case m: Map[_, _] =>
        JsObject(m.asInstanceOf[Map[String, Any]].mapValues(write))
      case l: List[Any] => JsArray(l.map(write).toVector)
      case n: Int       => JsNumber(n)
      case n: Long      => JsNumber(n)
      case n: Double    => JsNumber(n)
      case s: String    => JsString(s)
      case true         => JsTrue
      case false        => JsFalse
      case v: JsValue   => v
      case null         => JsNull
      case r            => JsString(r.toString)
    }

    def read(value: JsValue) = throw new UnsupportedOperationException()
  }

  class AnyJsonWriter extends JsonWriter[Map[String, Any]] {
    override def write(obj: Map[String, Any]): JsValue =
      AnyJsonFormat.write(obj)
  }

  class SeqAnyJsonWriter[T <: Any] extends JsonWriter[Seq[Map[String, T]]] {
    override def write(objs: Seq[Map[String, T]]): JsValue =
      new JsArray(
        objs
          .map(obj => {
            AnyJsonFormat.write(obj)
          })
          .toVector)
  }

}
