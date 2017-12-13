package cool.graph.api.mutations

import cool.graph.gc_values.GCValue
import cool.graph.shared.models._
import cool.graph.util.gc_value.{GCAnyConverter, GCDBValueConverter}

import scala.collection.immutable.Seq

/**
  * It's called CoolArgs to easily differentiate from Sangrias Args class.
  */
case class CoolArgs(raw: Map[String, Any]) {

//  def subArgsList2(field: Field): Option[Seq[CoolArgs]] = {
//    val fieldValues: Option[Seq[Map[String, Any]]] = field.isList match {
//      case true  => getFieldValuesAs[Map[String, Any]](field)
//      case false => getFieldValueAsSeq[Map[String, Any]](field.name)
//    }
//
//    fieldValues match {
//      case None    => None
//      case Some(x) => Some(x.map(CoolArgs(_)))
//    }
//  }

  def subArgsList(field: String): Option[Seq[CoolArgs]] = {
    getFieldValuesAs[Map[String, Any]](field) match {
      case None    => None
      case Some(x) => Some(x.map(CoolArgs(_)))
    }
  }

  def subArgs(field: Field): Option[Option[CoolArgs]] = subArgs(field.name)

  def subArgs(name: String): Option[Option[CoolArgs]] = {
    val fieldValue: Option[Option[Map[String, Any]]] = getFieldValueAs[Map[String, Any]](name)
    fieldValue match {
      case None          => None
      case Some(None)    => Some(None)
      case Some(Some(x)) => Some(Some(CoolArgs(x)))
    }
  }

  def hasArgFor(field: Field) = raw.get(field.name).isDefined

  /**
    * The outer option is defined if the field key was specified in the arguments at all.
    * The inner option is empty if a null value was sent for this field. If the option is defined it contains a non null value
    * for this field.
    */
  def getFieldValueAs[T](field: Field): Option[Option[T]] = getFieldValueAs(field.name)

  def getFieldValueAs[T](name: String): Option[Option[T]] = {
    raw.get(name).map { fieldValue =>
      try {
        fieldValue.asInstanceOf[Option[T]]
      } catch {
        case _: ClassCastException =>
          Option(fieldValue.asInstanceOf[T])
      }
    }
  }

  def getFieldValueAsSeq[T](name: String): Option[Seq[T]] = {
    raw.get(name).map { fieldValue =>
      try {
        fieldValue.asInstanceOf[Option[T]] match {
          case Some(x) => Seq(x)
          case None    => Seq.empty
        }
      } catch {
        case _: ClassCastException =>
          Seq(fieldValue.asInstanceOf[T])
      }
    }
  }

  /**
    * The outer option is defined if the field key was specified in the arguments at all.
    * The inner sequence then contains all the values specified.
    */
  def getFieldValuesAs[T](field: Field): Option[Seq[T]] = getFieldValuesAs(field.name)

  def getFieldValuesAs[T](field: String): Option[Seq[T]] = {
    raw.get(field).map { fieldValue =>
      try {
        fieldValue.asInstanceOf[Option[Seq[T]]].getOrElse(Seq.empty)
      } catch {
        case _: ClassCastException =>
          fieldValue.asInstanceOf[Seq[T]]
      }
    }
  }

  def extractNodeSelectorFromSangriaArgs(model: Model): NodeSelector = {
    val whereArgs = raw("where").asInstanceOf[Map[String, Option[Any]]]
    whereArgs.collectFirst {
      case (fieldName, Some(value)) =>
        NodeSelector(fieldName, GCAnyConverter(model.getFieldByName_!(fieldName).typeIdentifier, isList = false).toGCValue(value).get)
    } getOrElse {
      sys.error("You must specify a unique selector")
    }
  }

}

case class NodeSelector(fieldName: String, fieldValue: GCValue) {
  lazy val unwrappedFieldValue: Any = GCDBValueConverter().fromGCValue(fieldValue)
}
