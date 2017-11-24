package cool.graph.deploy.database.persistence

import cool.graph.shared.models._
import play.api.libs.json._

object MigrationStepsJsonFormatter extends DefaultReads {
  implicit val createModelFormat = Json.format[CreateModel]
  implicit val deleteModelFormat = Json.format[DeleteModel]
  implicit val updateModelFormat = Json.format[UpdateModel]

  implicit val createFieldFormat = Json.format[CreateField]
  implicit val deleteFieldFormat = Json.format[DeleteField]
//  implicit val updateFieldFormat = Json.format[UpdateField]

  implicit val createEnumFormat = Json.format[CreateEnum]
  implicit val deleteEnumFormat = Json.format[DeleteEnum]
  implicit val updateEnumFormat = Json.format[UpdateEnum]

  implicit val migrationStepFormat: Format[MigrationStep] = new Format[MigrationStep] {
    override def reads(json: JsValue): JsResult[MigrationStep] = ???

    override def writes(step: MigrationStep): JsValue = step match {
      case x: CreateModel => createModelFormat.writes(x)
      case x: DeleteModel => deleteModelFormat.writes(x)
      case x: UpdateModel => updateModelFormat.writes(x)
      case x: CreateField => createFieldFormat.writes(x)
      case x: DeleteField => deleteFieldFormat.writes(x)
      case x: UpdateField => updateFieldFormat.writes(x)
      case x: CreateEnum  => createEnumFormat.writes(x)
      case x: DeleteEnum  => deleteEnumFormat.writes(x)
      case x: UpdateEnum  => updateEnumFormat.writes(x)
    }
  }

  implicit val migrationStepsFormat: Format[MigrationSteps] = Json.format[MigrationSteps]

  implicit val updateFieldFormat = new OFormat[UpdateField] {
    val modelField        = "model"
    val nameField         = "name"
    val newNameField      = "newName"
    val typeNameField     = "typeName"
    val isRequiredField   = "isRequired"
    val isListField       = "isList"
    val isUniqueField     = "isUnique"
    val relationField     = "relation"
    val defaultValueField = "defaultValue"
    val enumField         = "enum"

    override def reads(json: JsValue): JsResult[UpdateField] = {
      for {
        model        <- (json \ modelField).validate[String]
        name         <- (json \ nameField).validate[String]
        newName      <- (json \ newNameField).validateOpt[String]
        typeName     <- (json \ typeNameField).validateOpt[String]
        isRequired   <- (json \ isRequiredField).validateOpt[Boolean]
        isList       <- (json \ isListField).validateOpt[Boolean]
        isUnique     <- (json \ isUniqueField).validateOpt[Boolean]
        relation     <- doubleOptReads[String](relationField)
        defaultValue <- doubleOptReads[String](defaultValueField)
        enum         <- doubleOptReads[String](enumField)
      } yield {
        UpdateField(
          model = model,
          name = name,
          newName = newName,
          typeName = typeName,
          isRequired = isRequired,
          isList = isList,
          isUnique = isUnique,
          relation = relation,
          defaultValue = defaultValue,
          enum = enum
        )
      }
    }

    override def writes(x: UpdateField): JsObject = {
      Json.obj(
        modelField      -> x.model,
        nameField       -> x.name,
        newNameField    -> x.newName,
        typeNameField   -> x.typeName,
        isRequiredField -> x.isRequired,
        isListField     -> x.isList,
        isUniqueField   -> x.isUnique
      ) ++ doubleOptWrites(relationField, x.relation) ++ doubleOptWrites(defaultValueField, x.defaultValue) ++ doubleOptWrites(enumField, x.enum)
    }
  }

  implicit def doubleOptReads[T](field: String)(implicit optReads: Reads[Option[T]]): Reads[Option[Option[T]]] = new Reads[Option[Option[T]]] {
    override def reads(json: JsValue): JsResult[Option[Option[T]]] = {
      json.validate[JsObject].flatMap { jsObject =>
        jsObject.value.get(field) match {
          case None          => JsSuccess(None)
          case Some(JsNull)  => JsSuccess(Some(None))
          case Some(jsValue) => jsValue.validate[T].map(v => Some(Some(v)))
        }
      }
    }
  }

  def doubleOptWrites[T](field: String, opt: Option[Option[T]])(implicit writes: Writes[T]): JsObject = {
    opt match {
      case Some(innerOpt) => JsObject(Vector(field -> Json.toJson(innerOpt)))
      case None           => JsObject.empty
    }
  }
}
