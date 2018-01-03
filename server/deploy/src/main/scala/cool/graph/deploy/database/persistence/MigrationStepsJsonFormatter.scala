package cool.graph.deploy.database.persistence

import cool.graph.shared.models._
import play.api.libs.json._

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

  implicit val createEnumFormat = Json.format[CreateEnum]
  implicit val deleteEnumFormat = Json.format[DeleteEnum]
  implicit val updateEnumFormat = Json.format[UpdateEnum]

  implicit val createRelationFormat = Json.format[CreateRelation]
  implicit val deleteRelationFormat = Json.format[DeleteRelation]
  implicit val updateRelationFormat = Json.format[UpdateRelation]

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
        case "UpdateRelation" => deleteRelationFormat.reads(json)
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

  implicit val migrationStepsFormat: Format[Migration] = Json.format[Migration]

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
}
