package cool.graph.api.mutations

import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.api.schema.APIErrors
import cool.graph.gc_values.{GCValue, GraphQLIdGCValue}
import cool.graph.shared.models._
import cool.graph.util.gc_value.{GCAnyConverter, GCValueExtractor}

import scala.collection.immutable.Seq

/**
  * It's called CoolArgs to easily differentiate from Sangrias Args class.
  */
case class CoolArgs(raw: Map[String, Any]) {
  def isEmpty: Boolean    = raw.isEmpty
  def isNonEmpty: Boolean = raw.nonEmpty

  def subNestedMutation(relationField: Field, subModel: Model): NestedMutation = {
    subArgsOption(relationField) match {
      case None             => NestedMutation.empty
      case Some(None)       => NestedMutation.empty
      case Some(Some(args)) => args.asNestedMutation(relationField, subModel)
    }
  }

  private def asNestedMutation(relationField: Field, subModel: Model): NestedMutation = {
    if (relationField.isList) {
      NestedMutation(
        creates = subArgsVector("create").getOrElse(Vector.empty).map(CreateOne),
        updates = subArgsVector("update").getOrElse(Vector.empty).map { args =>
          UpdateOne(args.extractNodeSelectorFromWhereField(subModel), args.subArgsOption("data").get.get)
        },
        upserts = subArgsVector("upsert").getOrElse(Vector.empty).map { args =>
          UpsertOne(
            where = args.extractNodeSelectorFromWhereField(subModel),
            update = args.subArgsOption("update").get.get,
            create = args.subArgsOption("create").get.get
          )
        },
        deletes = subArgsVector("delete").getOrElse(Vector.empty).map(args => DeleteOne(args.extractNodeSelector(subModel))),
        connects = subArgsVector("connect").getOrElse(Vector.empty).map(args => ConnectOne(args.extractNodeSelector(subModel))),
        disconnects = subArgsVector("disconnect").getOrElse(Vector.empty).map(args => DisconnectOne(args.extractNodeSelector(subModel)))
      )
    } else {
      NestedMutation(
        creates = subArgsOption("create").flatten.map(CreateOne).toVector,
        updates = subArgsOption("update").flatten.map { args =>
          UpdateOne(args.extractNodeSelectorFromWhereField(subModel), args.subArgsOption("data").get.get)
        }.toVector,
        upserts = subArgsOption("upsert").flatten.map { args =>
          UpsertOne(
            where = args.extractNodeSelectorFromWhereField(subModel),
            update = args.subArgsOption("update").get.get,
            create = args.subArgsOption("create").get.get
          )
        }.toVector,
        deletes = subArgsOption("delete").flatten.map(args => DeleteOne(args.extractNodeSelector(subModel))).toVector,
        connects = subArgsOption("connect").flatten.map(args => ConnectOne(args.extractNodeSelector(subModel))).toVector,
        disconnects = subArgsOption("disconnect").flatten.map(args => DisconnectOne(args.extractNodeSelector(subModel))).toVector
      )
    }
  }

  def subScalarList(scalarListField: Field): Option[ScalarListSet] = {
    subArgsOption(scalarListField).flatten.flatMap { args =>
      args.getFieldValuesAs[Any]("set") match {
        case None         => None
        case Some(values) => Some(ScalarListSet(values = values.toVector))
      }
    }

  }

  def nonListScalarArgumentsAsCoolArgs(model: Model): CoolArgs = {
    val argumentValues = nonListScalarArguments(model)
    val rawArgs        = argumentValues.map(x => x.name -> x.value).toMap
    CoolArgs(rawArgs)
  }

  def nonListScalarArguments(model: Model): Vector[ArgumentValue] = {
    for {
      field      <- model.scalarFields.toVector.filter(!_.isList)
      fieldValue <- getFieldValueAs[Any](field)
    } yield {
      ArgumentValue(field.name, fieldValue)
    }
  }

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

  def subArgsVector(field: String): Option[Vector[CoolArgs]] = subArgsList(field).map(_.toVector)

  def subArgsList(field: String): Option[Seq[CoolArgs]] = {
    getFieldValuesAs[Map[String, Any]](field) match {
      case None    => None
      case Some(x) => Some(x.map(CoolArgs))
    }
  }

  def subArgsOption(field: Field): Option[Option[CoolArgs]] = subArgsOption(field.name)

  def subArgsOption(name: String): Option[Option[CoolArgs]] = {
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

  def extractNodeSelectorFromWhereField(model: Model): NodeSelector = {
    val whereArgs = raw("where").asInstanceOf[Map[String, Option[Any]]]
    CoolArgs(whereArgs).extractNodeSelector(model)
  }

  def extractNodeSelector(model: Model): NodeSelector = {
    raw.asInstanceOf[Map[String, Option[Any]]].collectFirst {
      case (fieldName, Some(value)) =>
        NodeSelector(model,
                     model.getFieldByName_!(fieldName),
                     GCAnyConverter(model.getFieldByName_!(fieldName).typeIdentifier, isList = false).toGCValue(value).get)
    } getOrElse {
      throw APIErrors.NullProvidedForWhereError(model.name)
    }
  }

}

object NodeSelector {
  def forId(model: Model, id: String): NodeSelector = NodeSelector(model, model.getFieldByName_!("id"), GraphQLIdGCValue(id))
}

case class NodeSelector(model: Model, field: Field, fieldValue: GCValue) {
  lazy val unwrappedFieldValue: Any   = GCValueExtractor.fromGCValue(fieldValue)
  lazy val fieldValueAsString: String = GCValueExtractor.fromGCValueToString(fieldValue)

//  lazy val unwrappedFieldValue: Any   = {
//    fieldValue match {
//      case x: DateTimeGCValue => x.toMySqlDateTimeFormat
//      case _ => GCDBValueConverter().fromGCValue(fieldValue)
//    }
//  }
//  lazy val fieldValueAsString: String = fieldValue match {
//    case x: DateTimeGCValue => x.toMySqlDateTimeFormat
//    case _ => GCDBValueConverter().fromGCValueToString(fieldValue)
//  }
}
