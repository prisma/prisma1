package com.prisma.api.schema

import com.prisma.cache.Cache
import com.prisma.shared.models.{Field, Model, Project}
import sangria.schema._

trait InputTypesBuilder {
  def inputObjectTypeForCreate(model: Model, parentField: Option[Field] = None): Option[InputObjectType[Any]]

  def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]]

  def inputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]]
}

case class CachedInputTypesBuilder(project: Project) extends UncachedInputTypesBuilder(project) {
  import java.lang.{StringBuilder => JStringBuilder}

  val cache: Cache[String, Option[InputObjectType[Any]]] = Cache.unbounded[String, Option[InputObjectType[Any]]]()

  override def inputObjectTypeForCreate(model: Model, parentField: Option[Field]): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForCreate", model, parentField), { () =>
      computeInputObjectTypeForCreate(model, parentField)
    })
  }

  override def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForUpdate", model), { () =>
      computeInputObjectTypeForUpdate(model)
    })
  }

  private def cacheKey(name: String, model: Model, field: Option[Field] = None): String = {
    val sb = new JStringBuilder()

    sb.append(name)
    sb.append(model.name)
    sb.append(field.orNull)
    sb.toString
  }
}

abstract class UncachedInputTypesBuilder(project: Project) extends InputTypesBuilder {
  override def inputObjectTypeForCreate(model: Model, parentField: Option[Field]): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForCreate(model, parentField)
  }

  override def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForUpdate(model)
  }

  override def inputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForWhereUnique(model)
  }

  protected def computeInputObjectTypeForCreate(model: Model, parentField: Option[Field]): Option[InputObjectType[Any]] = {
    val inputObjectTypeName = parentField.flatMap(_.otherRelationField) match {
      case Some(field) if !field.isHidden => s"${model.name}CreateWithout${field.name.capitalize}Input"
      case _                              => s"${model.name}CreateInput"
    }

    val fields = computeScalarInputFieldsForCreate(model) ++ computeRelationalInputFieldsForCreate(model, parentField)

    if (fields.nonEmpty) {
      Some(
        InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () => {
            fields
          }
        )
      )
    } else {
      None
    }
  }

  protected def computeInputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    val fields = computeScalarInputFieldsForUpdate(model) ++ computeRelationalInputFieldsForUpdate(model, parentField = None)

    if (fields.nonEmpty) {
      Some(
        InputObjectType[Any](
          name = s"${model.name}UpdateInput",
          fieldsFn = () => {
            fields
          }
        )
      )
    } else {
      None
    }
  }

  protected def computeInputObjectTypeForNestedUpdate(parentField: Field): Option[InputObjectType[Any]] = {
    val subModel = parentField.relatedModel_!
    computeInputObjectTypeForNestedUpdateData(parentField).flatMap { updateDataInput =>
      if (parentField.isList) {
        for {
          whereArg <- computeInputObjectTypeForWhereUnique(subModel)
        } yield {
          val typeName = parentField.otherRelationField match {
            case Some(field) if !field.isHidden => s"${subModel.name}UpdateWithWhereUniqueWithout${field.name.capitalize}Input"
            case _                              => s"${subModel.name}UpdateWithWhereUniqueNestedInput"
          }

          InputObjectType[Any](
            name = typeName,
            fieldsFn = () => {
              List(
                InputField[Any]("where", whereArg),
                InputField[Any]("data", updateDataInput)
              )
            }
          )
        }
      } else {
        Some(updateDataInput)
      }
    }
  }

  protected def computeInputObjectTypeForNestedUpdateData(parentField: Field): Option[InputObjectType[Any]] = {
    val subModel = parentField.relatedModel_!
    val fields   = computeScalarInputFieldsForUpdate(subModel) ++ computeRelationalInputFieldsForUpdate(subModel, parentField = Some(parentField))

    if (fields.nonEmpty) {
      val typeName = parentField.otherRelationField match {
        case Some(field) if !field.isHidden => s"${subModel.name}UpdateWithout${field.name.capitalize}DataInput"
        case _                              => s"${subModel.name}UpdateDataInput"
      }

      Some(
        InputObjectType[Any](
          name = typeName,
          fieldsFn = () => {
            fields
          }
        )
      )
    } else {
      None
    }
  }

  protected def computeInputObjectTypeForNestedUpsert(parentField: Field): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForNestedUpdateData(parentField).flatMap { updateDataInput =>
      if (parentField.isList) {
        computeInputObjectTypeForNestedUpsertList(parentField, updateDataInput)
      } else {
        computeInputObjectTypeForNestedUpsertNonList(parentField, updateDataInput)
      }
    }
  }

  private def computeInputObjectTypeForNestedUpsertNonList(parentField: Field, updateDataInput: InputObjectType[Any]) = {
    val subModel = parentField.relatedModel_!

    for {
      createArg <- computeInputObjectTypeForCreate(subModel, Some(parentField))
    } yield {
      val typeName = parentField.otherRelationField match {
        case Some(field) if !field.isHidden => s"${subModel.name}UpsertWithout${field.name.capitalize}Input"
        case _                              => s"${subModel.name}UpsertNestedInput"
      }

      InputObjectType[Any](
        name = typeName,
        fieldsFn = () => {
          List(
            InputField[Any]("update", updateDataInput),
            InputField[Any]("create", createArg)
          )
        }
      )
    }
  }

  private def computeInputObjectTypeForNestedUpsertList(parentField: Field, updateDataInput: InputObjectType[Any]) = {
    val subModel = parentField.relatedModel_!

    for {
      whereArg  <- computeInputObjectTypeForWhereUnique(subModel)
      createArg <- computeInputObjectTypeForCreate(subModel, Some(parentField))
    } yield {
      val typeName = parentField.otherRelationField match {
        case Some(field) if !field.isHidden => s"${subModel.name}UpsertWithWhereUniqueWithout${field.name.capitalize}Input"
        case _                              => s"${subModel.name}UpsertWithWhereUniqueNestedInput"
      }

      InputObjectType[Any](
        name = typeName,
        fieldsFn = () => {
          List(
            InputField[Any]("where", whereArg),
            InputField[Any]("update", updateDataInput),
            InputField[Any]("create", createArg)
          )
        }
      )
    }
  }

  protected def computeInputObjectTypeForWhere(model: Model): InputObjectType[Any] = FilterObjectTypeBuilder(model, project).filterObjectType

  protected def computeInputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]] = {
    val uniqueFields = model.fields.filter(f => f.isUnique && f.isVisible)

    if (uniqueFields.isEmpty) {
      None
    } else {
      Some(
        InputObjectType[Any](
          name = s"${model.name}WhereUniqueInput",
          fieldsFn = () => {
            uniqueFields.map { field =>
              InputField(name = field.name, fieldType = SchemaBuilderUtils.mapToOptionalInputType(field))
            }
          }
        ))
    }
  }

  private def computeScalarInputFieldsForCreate(model: Model) = {
    val filteredModel = model.filterFields(_.isWritable)
    computeScalarInputFields(filteredModel, FieldToInputTypeMapper.mapForCreateCase, "Create")
  }

  private def computeScalarInputFieldsForUpdate(model: Model) = {
    val filteredModel = model.filterFields(f => f.isWritable)
    computeScalarInputFields(filteredModel, SchemaBuilderUtils.mapToOptionalInputType, "Update")
  }

  private def computeScalarInputFields(model: Model, mapToInputType: Field => InputType[Any], inputObjectName: String) = {
    val nonListFields = model.scalarNonListFields.map(field => InputField(field.name, mapToInputType(field)))

    val listFields = model.scalarListFields.map { field =>
      val setField =
        OptionInputType(
          InputObjectType(
            name = s"${model.name}$inputObjectName${field.name}Input",
            fieldsFn = () => List(InputField(name = "set", fieldType = SchemaBuilderUtils.mapToOptionalInputType(field)))
          ))

      InputField(field.name, setField)
    }

    nonListFields ++ listFields
  }

  private def computeRelationalInputFieldsForUpdate(model: Model, parentField: Option[Field]): List[InputField[Any]] = {
    model.visibleRelationFields.flatMap { field =>
      val subModel     = field.relatedModel_!
      val relatedField = field.otherRelationField

      val inputObjectTypeName = {
        val arityPart = if (field.isList) "Many" else "One"
        val withoutPart = relatedField match {
          case Some(field) if !field.isHidden => s"Without${field.name.capitalize}"
          case _                              => ""
        }
        s"${subModel.name}Update${arityPart}${withoutPart}Input"
      }

      val fieldIsOppositeRelationField = parentField.flatMap(_.otherRelationField).contains(field)

      if (fieldIsOppositeRelationField) {
        None
      } else {

        val disconnectIfPossible = if (!field.isList && field.isRequired) None else nestedDisconnectInputField(field)

        val inputObjectType = InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () =>
            nestedCreateInputField(field).toList ++
              nestedConnectInputField(field) ++
              disconnectIfPossible ++
              nestedDeleteInputField(field) ++
              nestedUpdateInputField(field) ++
              nestedUpsertInputField(field)
        )
        Some(InputField[Any](field.name, OptionInputType(inputObjectType)))
      }
    }
  }

  private def computeRelationalInputFieldsForCreate(model: Model, parentField: Option[Field]): List[InputField[Any]] = {
    model.visibleRelationFields.flatMap { field =>
      val subModel     = field.relatedModel_!
      val relatedField = field.otherRelationField

      val inputObjectTypeName = {
        val arityPart = if (field.isList) "Many" else "One"
        val withoutPart = relatedField match {
          case Some(field) if !field.isHidden => s"Without${field.name.capitalize}"
          case _                              => ""
        }
        s"${subModel.name}Create${arityPart}${withoutPart}Input"
      }

      val fieldIsOppositeRelationField = parentField.flatMap(_.otherRelationField).contains(field)

      if (fieldIsOppositeRelationField) {
        None
      } else {
        val inputObjectType = InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () => nestedCreateInputField(field).toList ++ nestedConnectInputField(field)
        )
        val possiblyRequired = if (field.isRequired) {
          inputObjectType
        } else {
          OptionInputType(inputObjectType)
        }

        Some(InputField[Any](field.name, possiblyRequired))
      }
    }
  }

  def nestedUpdateInputField(field: Field): Option[InputField[Any]] = {
    val inputObjectType = computeInputObjectTypeForNestedUpdate(field)
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("update", x))
  }

  def nestedCreateInputField(field: Field): Option[InputField[Any]] = {
    val subModel        = field.relatedModel_!
    val inputObjectType = inputObjectTypeForCreate(subModel, Some(field))

    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("create", x))
  }

  def nestedUpsertInputField(field: Field): Option[InputField[Any]] = {
    val inputObjectType = computeInputObjectTypeForNestedUpsert(field)
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("upsert", x))
  }

  def nestedConnectInputField(field: Field): Option[InputField[Any]] = whereInputField(field, name = "connect")

  def nestedDisconnectInputField(field: Field): Option[InputField[Any]] = field.isList match {
    case true  => whereInputField(field, name = "disconnect")
    case false => Some(InputField[Any]("disconnect", OptionInputType(BooleanType)))
  }

  def nestedDeleteInputField(field: Field): Option[InputField[Any]] = field.isList match {
    case true  => whereInputField(field, name = "delete")
    case false => Some(InputField[Any]("delete", OptionInputType(BooleanType)))
  }

  def trueInputFlag(field: Field, name: String): Option[InputField[Any]] = {
    val subModel        = field.relatedModel_!
    val inputObjectType = inputObjectTypeForWhereUnique(subModel)

    generateInputType(inputObjectType, field.isList).map(x => InputField[Any](name, x))
  }

  def whereInputField(field: Field, name: String): Option[InputField[Any]] = {
    val subModel        = field.relatedModel_!
    val inputObjectType = inputObjectTypeForWhereUnique(subModel)

    generateInputType(inputObjectType, field.isList).map(x => InputField[Any](name, x))
  }

  private def generateInputType(input: Option[InputObjectType[Any]], isList: Boolean) = isList match {
    case true  => input.map(x => OptionInputType(ListInputType(x)))
    case false => input.map(x => OptionInputType(x))
  }
}

object FieldToInputTypeMapper {
  def mapForCreateCase(field: Field): InputType[Any] = field.isRequired && field.defaultValue.isEmpty match {
    case true  => SchemaBuilderUtils.mapToRequiredInputType(field)
    case false => SchemaBuilderUtils.mapToOptionalInputType(field)
  }
}
