package cool.graph.util.coolSangria

import sangria.marshalling.{CoercedScalaResultMarshaller, FromInput, ResultMarshaller}

object FromInputImplicit {

  implicit val DefaultScalaResultMarshaller: FromInput[Any] = new FromInput[Any] {
    override val marshaller: ResultMarshaller           = ResultMarshaller.defaultResultMarshaller
    override def fromResult(node: marshaller.Node): Any = node
  }

  implicit val CoercedResultMarshaller: FromInput[Any] = new FromInput[Any] {
    override val marshaller: ResultMarshaller           = CoercedScalaResultMarshaller.default
    override def fromResult(node: marshaller.Node): Any = node
  }
}
