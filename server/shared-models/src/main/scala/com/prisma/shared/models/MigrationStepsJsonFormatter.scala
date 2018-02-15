package com.prisma.shared.models

import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.language.implicitConversions

object MigrationStepsJsonFormatter extends DefaultReads {
  implicit val createModelFormat = Json.format[CreateModel]
  implicit val deleteModelFormat = Json.format[DeleteModel]
  implicit val updateModelFormat = Json.format[UpdateModel]

  implicit val createFieldFormat = Json.format[CreateField]
  implicit val deleteFieldFormat = Json.format[DeleteField]
  implicit val updateFieldFormat = new OFormat[UpdateField] {
    val modelField        = "model"
    val nameField         = "name"
    val newNameField      = "newName"
    val typeNameField     = "typeName"
    val isRequiredField   = "isRequired"
    val isListField       = "isList"
    val isUniqueField     = "unique"
    val isHiddenField     = "isHidden"
    val relationField     = "relation"
    val defaultValueField = "default"
    val enumField         = "enum"

    override def reads(json: JsValue): JsResult[UpdateField] = {
      for {
        model        <- (json \ modelField).validate[String]
        name         <- (json \ nameField).validate[String]
        newName      <- (json \ newNameField).validateOpt[String]
        typeName     <- (json \ typeNameField).validateOpt[String]
        isRequired   <- (json \ isRequiredField).validateOpt[Boolean]
        isList       <- (json \ isListField).validateOpt[Boolean]
        isHidden     <- (json \ isHiddenField).validateOpt[Boolean]
        isUnique     <- (json \ isUniqueField).validateOpt[Boolean]
        relation     <- (json \ relationField).validateDoubleOpt[String]
        defaultValue <- (json \ defaultValueField).validateDoubleOpt[String]
        enum         <- (json \ enumField).validateDoubleOpt[String]
      } yield {
        UpdateField(
          model = model,
          name = name,
          newName = newName,
          typeName = typeName,
          isRequired = isRequired,
          isList = isList,
          isUnique = isUnique,
          isHidden = isHidden,
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
      ) ++ writeDoubleOpt(relationField, x.relation) ++ writeDoubleOpt(defaultValueField, x.defaultValue) ++ writeDoubleOpt(enumField, x.enum)
    }
  }

  object EnumUtils {
    def enumReads[E <: Enumeration](enum: E): Reads[E#Value] = new Reads[E#Value] {
      def reads(json: JsValue): JsResult[E#Value] = json match {
        case JsString(s) =>
          try {
            JsSuccess(enum.withName(s))
          } catch {
            case _: NoSuchElementException => JsError(s"Enumeration expected of type: '${enum.getClass}', but it does not appear to contain the value: '$s'")
          }
        case _ => JsError("String value expected")
      }
    }

    implicit def enumWrites[E <: Enumeration]: Writes[E#Value] = new Writes[E#Value] {
      def writes(v: E#Value): JsValue = JsString(v.toString)
    }

    implicit def enumFormat[E <: Enumeration](enum: E): Format[E#Value] = {
      Format(EnumUtils.enumReads(enum), EnumUtils.enumWrites)
    }
  }

  implicit val onDeleteEnumTypeFormat = EnumUtils.enumFormat(OnDelete)

  implicit val createEnumFormat = Json.format[CreateEnum]
  implicit val deleteEnumFormat = Json.format[DeleteEnum]
  implicit val updateEnumFormat = Json.format[UpdateEnum]

  implicit val createRelationFormat: OFormat[CreateRelation] = {
    val reads = (
      (JsPath \ "name").read[String] and
        readOneOf[String]("leftModelName", "modelAName") and
        readOneOf[String]("rightModelName", "modelBName") and
        (JsPath \ "modelAOnDelete").readWithDefault(OnDelete.SetNull) and
        (JsPath \ "modelBOnDelete").readWithDefault(OnDelete.SetNull)
    )(CreateRelation.apply _)

    val writes = (
      (JsPath \ "name").write[String] and
        (JsPath \ "leftModelName").write[String] and
        (JsPath \ "rightModelName").write[String] and
        (JsPath \ "modelAOnDelete").write[OnDelete.Value] and
        (JsPath \ "modelBOnDelete").write[OnDelete.Value]
    )(unlift(CreateRelation.unapply))

    OFormat(reads, writes)
  }

  implicit val deleteRelationFormat: OFormat[DeleteRelation] = {
    val reads  = (JsPath \ "name").read[String].map(DeleteRelation.apply)
    val writes = OWrites[DeleteRelation](delete => Json.obj("name" -> delete.name))
    OFormat(reads, writes)
  }

  implicit val updateRelationFormat: OFormat[UpdateRelation] = {
    val format: OFormat[UpdateRelation] = (
      (JsPath \ "name").format[String] and
        (JsPath \ "newName").formatNullable[String] and
        (JsPath \ "modelAId").formatNullable[String] and
        (JsPath \ "modelBId").formatNullable[String] and
        (JsPath \ "modelAOnDelete").formatNullable[OnDelete.Value] and
        (JsPath \ "modelBOnDelete").formatNullable[OnDelete.Value]
    )(UpdateRelation.apply, unlift(UpdateRelation.unapply))

    format
  }

  implicit val migrationStepFormat: Format[MigrationStep] = new Format[MigrationStep] {
    val discriminatorField = "discriminator"

    override def reads(json: JsValue): JsResult[MigrationStep] = {
      (json \ discriminatorField).validate[String].flatMap {
        case "CreateModel"    => createModelFormat.reads(json)
        case "DeleteModel"    => deleteModelFormat.reads(json)
        case "UpdateModel"    => updateModelFormat.reads(json)
        case "CreateField"    => createFieldFormat.reads(json)
        case "DeleteField"    => deleteFieldFormat.reads(json)
        case "UpdateField"    => updateFieldFormat.reads(json)
        case "CreateEnum"     => createEnumFormat.reads(json)
        case "DeleteEnum"     => deleteEnumFormat.reads(json)
        case "UpdateEnum"     => updateEnumFormat.reads(json)
        case "CreateRelation" => createRelationFormat.reads(json)
        case "DeleteRelation" => deleteRelationFormat.reads(json)
        case "UpdateRelation" => updateRelationFormat.reads(json)
      }
    }

    override def writes(step: MigrationStep): JsValue = {
      val withOutDiscriminator = step match {
        case x: CreateModel    => createModelFormat.writes(x)
        case x: DeleteModel    => deleteModelFormat.writes(x)
        case x: UpdateModel    => updateModelFormat.writes(x)
        case x: CreateField    => createFieldFormat.writes(x)
        case x: DeleteField    => deleteFieldFormat.writes(x)
        case x: UpdateField    => updateFieldFormat.writes(x)
        case x: CreateEnum     => createEnumFormat.writes(x)
        case x: DeleteEnum     => deleteEnumFormat.writes(x)
        case x: UpdateEnum     => updateEnumFormat.writes(x)
        case x: CreateRelation => createRelationFormat.writes(x)
        case x: DeleteRelation => deleteRelationFormat.writes(x)
        case x: UpdateRelation => updateRelationFormat.writes(x)
      }
      withOutDiscriminator ++ Json.obj(discriminatorField -> step.getClass.getSimpleName)
    }
  }

  def writeDoubleOpt[T](field: String, opt: Option[Option[T]])(implicit writes: Writes[T]): JsObject = {
    opt match {
      case Some(innerOpt) => JsObject(Vector(field -> Json.toJson(innerOpt)))
      case None           => JsObject.empty
    }
  }

  implicit class JsLookupResultExtension(jsLookUp: JsLookupResult) {
    def validateDoubleOpt[T](implicit rds: Reads[T]): JsResult[Option[Option[T]]] = jsLookUp match {
      case JsUndefined()     => JsSuccess(None)
      case JsDefined(JsNull) => JsSuccess(Some(None))
      case JsDefined(json)   => rds.reads(json).map(v => Some(Some(v)))
    }
  }

  def readOneOf[T](field1: String, field2: String)(implicit reads: Reads[T]) = (JsPath \ field1).read[T].orElse((JsPath \ field2).read[T])
}
