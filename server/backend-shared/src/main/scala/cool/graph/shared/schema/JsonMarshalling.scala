package cool.graph.shared.schema

import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import sangria.marshalling.{ArrayMapBuilder, InputUnmarshaller, ResultMarshaller, ScalarValueInfo}
import spray.json.{JsArray, JsBoolean, JsNull, JsNumber, JsObject, JsString, JsValue}

object JsonMarshalling {

  implicit object CustomSprayJsonResultMarshaller extends ResultMarshaller {
    type Node       = JsValue
    type MapBuilder = ArrayMapBuilder[Node]

    def emptyMapNode(keys: Seq[String]) = new ArrayMapBuilder[Node](keys)

    def addMapNodeElem(builder: MapBuilder, key: String, value: Node, optional: Boolean) = builder.add(key, value)

    def mapNode(builder: MapBuilder) = JsObject(builder.toMap)

    def mapNode(keyValues: Seq[(String, JsValue)]) = JsObject(keyValues: _*)

    def arrayNode(values: Vector[JsValue]) = JsArray(values)

    def optionalArrayNodeValue(value: Option[JsValue]) = value match {
      case Some(v) ⇒ v
      case None    ⇒ nullNode
    }

    def scalarNode(value: Any, typeName: String, info: Set[ScalarValueInfo]) =
      value match {
        case v: String     ⇒ JsString(v)
        case v: Boolean    ⇒ JsBoolean(v)
        case v: Int        ⇒ JsNumber(v)
        case v: Long       ⇒ JsNumber(v)
        case v: Float      ⇒ JsNumber(v)
        case v: Double     ⇒ JsNumber(v)
        case v: BigInt     ⇒ JsNumber(v)
        case v: BigDecimal ⇒ JsNumber(v)
        case v: DateTime   ⇒ JsString(v.toString(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z").withZoneUTC()))
        case v: JsValue    ⇒ v
        case v             ⇒ throw new IllegalArgumentException("Unsupported scalar value in CustomSprayJsonResultMarshaller: " + v)
      }

    def enumNode(value: String, typeName: String) = JsString(value)

    def nullNode = JsNull

    def renderCompact(node: JsValue) = node.compactPrint

    def renderPretty(node: JsValue) = node.prettyPrint
  }

  implicit object SprayJsonInputUnmarshaller extends InputUnmarshaller[JsValue] {

    def getRootMapValue(node: JsValue, key: String): Option[JsValue] = node.asInstanceOf[JsObject].fields get key

    def isListNode(node: JsValue) = node.isInstanceOf[JsArray]

    def getListValue(node: JsValue) = node.asInstanceOf[JsArray].elements

    def isMapNode(node: JsValue) = node.isInstanceOf[JsObject]

    def getMapValue(node: JsValue, key: String) = node.asInstanceOf[JsObject].fields get key

    def getMapKeys(node: JsValue) = node.asInstanceOf[JsObject].fields.keys

    def isDefined(node: JsValue) = node != JsNull

    def getScalarValue(node: JsValue): Any = node match {
      case JsBoolean(b) ⇒ b
      case JsNumber(d)  ⇒ d.toBigIntExact getOrElse d
      case JsString(s)  ⇒ s
      case n            ⇒ n
    }

    def getScalaScalarValue(node: JsValue) = getScalarValue(node)

    def isEnumNode(node: JsValue) = node.isInstanceOf[JsString]

    def isScalarNode(node: JsValue) = true

    def isVariableNode(node: JsValue) = false

    def getVariableName(node: JsValue) = throw new IllegalArgumentException("variables are not supported")

    def render(node: JsValue) = node.compactPrint
  }
}
