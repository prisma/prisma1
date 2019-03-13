package com.prisma.api.schema

import com.prisma.cache.Cache
import com.prisma.cache.factory.CacheFactory
import com.prisma.shared.models.FieldBehaviour.{IdBehaviour, IdStrategy}
import com.prisma.shared.models._
import sangria.schema.{Field => _, _}

trait InputTypesBuilder {
  def inputObjectTypeForCreate(model: Model, parentField: Option[RelationField] = None): Option[InputObjectType[Any]]

  def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]]

  def inputObjectTypeForUpdateMany(model: Model): Option[InputObjectType[Any]]

  def inputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]]
}

case class CachedInputTypesBuilder(project: Project, cacheFactory: CacheFactory) extends UncachedInputTypesBuilder(project) {
  import java.lang.{StringBuilder => JStringBuilder}

  val cache: Cache[String, Option[InputObjectType[Any]]] = cacheFactory.unbounded[String, Option[InputObjectType[Any]]]()

  override def inputObjectTypeForCreate(model: Model, parentField: Option[RelationField]): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForCreate", model, parentField), { () =>
      computeInputObjectTypeForCreate(model, parentField)
    })
  }

  override def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForUpdate", model), { () =>
      computeInputObjectTypeForUpdate(model)
    })
  }

  override def inputObjectTypeForUpdateMany(model: Model): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForUpdateMany", model), { () =>
      computeInputObjectTypeForUpdateMany(model)
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

  override def inputObjectTypeForCreate(model: Model, parentField: Option[RelationField]): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForCreate(model, parentField)
  }

  override def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForUpdate(model)
  }

  override def inputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForWhereUnique(model)
  }

  protected def computeInputObjectTypeForCreate(model: Model, parentField: Option[RelationField]): Option[InputObjectType[Any]] = {
    val inputObjectTypeName = parentField.map(_.relatedField) match {
      case Some(field) if !field.isHidden => s"${model.name}CreateWithout${field.name.capitalize}Input"
      case _                              => s"${model.name}CreateInput"
    }

    val fields = computeScalarInputFieldsForCreate(model) ++ computeRelationalInputFieldsForCreate(model, parentField)

    if (fields.nonEmpty) {
      Some(
        InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () => fields
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

  protected def computeInputObjectTypeForUpdateMany(model: Model): Option[InputObjectType[Any]] = {
    val fields = computeScalarInputFieldsForUpdate(model)

    if (fields.nonEmpty) Some(InputObjectType[Any](name = s"${model.name}UpdateManyMutationInput", fieldsFn = () => fields)) else None
  }

  protected def computeInputObjectTypeForNestedUpdate(parentField: RelationField): Option[InputObjectType[Any]] = {
    val subModel = parentField.relatedModel_!
    computeInputObjectTypeForNestedUpdateData(parentField).flatMap { updateDataInput =>
      if (parentField.isList) {
        for {
          whereArg <- computeInputObjectTypeForWhereUnique(subModel)
        } yield {
          val typeName = parentField.relatedField.isHidden match {
            case false => s"${subModel.name}UpdateWithWhereUniqueWithout${parentField.relatedField.name.capitalize}Input"
            case true  => s"${subModel.name}UpdateWithWhereUniqueNestedInput"
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

  protected def computeInputObjectTypeForNestedUpdateMany(parentField: RelationField): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForNestedUpdateManyData(parentField).flatMap { updateManyDataInput =>
      val subModel = parentField.relatedModel_!
      if (parentField.isList) {
        val typeName = s"${subModel.name}UpdateManyWithWhereNestedInput"

        Some(
          InputObjectType[Any](
            name = typeName,
            fieldsFn = () => {
              List(
                InputField[Any]("where", FilterObjectTypeBuilder(subModel, project).scalarFilterObjectType.get),
                InputField[Any]("data", updateManyDataInput)
              )
            }
          ))
      } else {
        None
      }
    }
  }

  protected def computeInputObjectTypeForNestedDeleteMany(parentField: RelationField): Option[InputObjectType[Any]] = {
    if (parentField.isList) FilterObjectTypeBuilder(parentField.relatedModel_!, project).scalarFilterObjectType else None
  }

  protected def computeInputObjectTypeForNestedUpdateData(parentField: RelationField): Option[InputObjectType[Any]] = {
    val subModel = parentField.relatedModel_!
    val fields   = computeScalarInputFieldsForUpdate(subModel) ++ computeRelationalInputFieldsForUpdate(subModel, parentField = Some(parentField))

    if (fields.nonEmpty) {
      val typeName = parentField.relatedField.isHidden match {
        case false => s"${subModel.name}UpdateWithout${parentField.relatedField.name.capitalize}DataInput"
        case true  => s"${subModel.name}UpdateDataInput"
      }

      Some(
        InputObjectType[Any](
          name = typeName,
          fieldsFn = () => { fields }
        )
      )
    } else {
      None
    }
  }

  protected def computeInputObjectTypeForNestedUpdateManyData(parentField: RelationField): Option[InputObjectType[Any]] = {
    val subModel = parentField.relatedModel_!
    val fields   = computeScalarInputFieldsForUpdate(subModel)

    if (fields.nonEmpty) {
      val typeName = s"${subModel.name}UpdateManyDataInput"

      Some(
        InputObjectType[Any](
          name = typeName,
          fieldsFn = () => { fields }
        )
      )
    } else {
      None
    }
  }

  protected def computeInputObjectTypeForNestedUpsert(parentField: RelationField): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForNestedUpdateData(parentField).flatMap { updateDataInput =>
      if (parentField.isList) {
        computeInputObjectTypeForNestedUpsertList(parentField, updateDataInput)
      } else {
        computeInputObjectTypeForNestedUpsertNonList(parentField, updateDataInput)
      }
    }
  }

  private def computeInputObjectTypeForNestedUpsertNonList(parentField: RelationField, updateDataInput: InputObjectType[Any]) = {
    val subModel = parentField.relatedModel_!

    for {
      createArg <- computeInputObjectTypeForCreate(subModel, Some(parentField))
    } yield {
      val typeName = parentField.relatedField.isHidden match {
        case false => s"${subModel.name}UpsertWithout${parentField.relatedField.name.capitalize}Input"
        case true  => s"${subModel.name}UpsertNestedInput"
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

  private def computeInputObjectTypeForNestedUpsertList(parentField: RelationField, updateDataInput: InputObjectType[Any]) = {
    val subModel = parentField.relatedModel_!

    for {
      whereArg  <- computeInputObjectTypeForWhereUnique(subModel)
      createArg <- computeInputObjectTypeForCreate(subModel, Some(parentField))
    } yield {
      val typeName = parentField.relatedField.isHidden match {
        case false => s"${subModel.name}UpsertWithWhereUniqueWithout${parentField.relatedField.name.capitalize}Input"
        case true  => s"${subModel.name}UpsertWithWhereUniqueNestedInput"
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

  protected def computeInputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]] = {
    val uniqueFields = model.scalarFields.filter(f => f.isUnique && f.isVisible)

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

  private def filterID(scalarField: ScalarField): Boolean = (scalarField.behaviour, scalarField.typeIdentifier) match {
    case _ if !scalarField.isId                                                                    => true
    case (Some(IdBehaviour(IdStrategy.Auto, _)) | None, TypeIdentifier.Int)                        => false
    case (Some(IdBehaviour(IdStrategy.Auto, _)) | None, TypeIdentifier.UUID | TypeIdentifier.Cuid) => true
    case (Some(IdBehaviour(IdStrategy.None, _)), TypeIdentifier.Int)                               => false
    case (Some(IdBehaviour(IdStrategy.None, _)), TypeIdentifier.UUID | TypeIdentifier.Cuid)        => true
    case (Some(IdBehaviour(IdStrategy.Sequence, _)), TypeIdentifier.Int)                           => false
    case _                                                                                         => sys.error("Id Behaviour unhandled")
  }

  private def computeScalarInputFieldsForCreate(model: Model) = {
    val filteredModel = model.filterScalarFields(x => !x.isCreatedAt && !x.isUpdatedAt && filterID(x))
    computeScalarInputFields(filteredModel, FieldToInputTypeMapper.mapForCreateCase, "Create")
  }

  private def computeScalarInputFieldsForUpdate(model: Model) = {
    val filteredModel = model.filterScalarFields(f => f.isWritable)
    computeScalarInputFields(filteredModel, SchemaBuilderUtils.mapToOptionalInputType, "Update")
  }

  private def computeScalarInputFields(model: Model, mapToInputType: ScalarField => InputType[Any], inputObjectName: String) = {
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

  private def computeRelationalInputFieldsForUpdate(model: Model, parentField: Option[RelationField]): List[InputField[Any]] = {
    model.visibleRelationFields.flatMap { field =>
      val subModel     = field.relatedModel_!
      val relatedField = field.relatedField

      val inputObjectTypeName = {
        val arityAndRequiredPart = (field.isList, field.isRequired) match {
          case (true, _)      => "Many"
          case (false, true)  => "OneRequired"
          case (false, false) => "One"
        }

        val withoutPart = relatedField.isHidden match {
          case false => s"Without${relatedField.name.capitalize}"
          case true  => ""
        }
        s"${subModel.name}Update${arityAndRequiredPart}${withoutPart}Input"
      }

      val fieldIsOppositeRelationField = parentField.map(_.relatedField).contains(field)

      if (fieldIsOppositeRelationField) {
        None
      } else {

        val inputObjectType = InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () =>
            nestedCreateInputField(field).toList ++
              nestedConnectInputField(field) ++
              nestedSetInputField(field) ++
              nestedDisconnectInputField(field) ++
              nestedDeleteInputField(field) ++
              nestedUpdateInputField(field) ++
              nestedUpdateManyInputField(field) ++
              nestedDeleteManyInputField(field) ++
              nestedUpsertInputField(field)
        )
        Some(InputField[Any](field.name, OptionInputType(inputObjectType)))
      }
    }
  }

  private def computeRelationalInputFieldsForCreate(model: Model, parentField: Option[RelationField]): List[InputField[Any]] = {
    model.visibleRelationFields.flatMap { field =>
      val subModel     = field.relatedModel_!
      val relatedField = field.relatedField

      val inputObjectTypeName = {
        val arityPart   = if (field.isList) "Many" else "One"
        val withoutPart = if (!relatedField.isHidden) s"Without${relatedField.name.capitalize}" else ""

        s"${subModel.name}Create${arityPart}${withoutPart}Input"
      }

      val fieldIsOppositeRelationField = parentField.map(_.relatedField).contains(field)

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

  def nestedUpdateInputField(field: RelationField): Option[InputField[Any]] = {
    val inputObjectType = computeInputObjectTypeForNestedUpdate(field)
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("update", x))
  }

  def nestedUpdateManyInputField(field: RelationField): Option[InputField[Any]] = {
    val inputObjectType = computeInputObjectTypeForNestedUpdateMany(field)
    inputObjectType.map(x => OptionInputType(ListInputType(x))).map(x => InputField[Any]("updateMany", x))
  }

  def nestedDeleteManyInputField(field: RelationField): Option[InputField[Any]] = {
    val inputObjectType = computeInputObjectTypeForNestedDeleteMany(field)
    inputObjectType.map(x => OptionInputType(ListInputType(x))).map(x => InputField[Any]("deleteMany", x))
  }

  def nestedCreateInputField(field: RelationField): Option[InputField[Any]] = {
    val inputObjectType = inputObjectTypeForCreate(field.relatedModel_!, Some(field))
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("create", x))
  }

  def nestedUpsertInputField(field: RelationField): Option[InputField[Any]] = {
    val inputObjectType = computeInputObjectTypeForNestedUpsert(field)
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("upsert", x))
  }

  def nestedConnectInputField(field: RelationField): Option[InputField[Any]] = field.relatedModel_!.isEmbedded match {
    case true  => None
    case false => whereInputField(field, name = "connect")
  }

  def nestedSetInputField(field: RelationField): Option[InputField[Any]] = (field.relatedModel_!.isEmbedded, field.isList) match {
    case (true, _)      => None
    case (false, true)  => whereInputField(field, name = "set")
    case (false, false) => None
  }

  def nestedDisconnectInputField(field: RelationField): Option[InputField[Any]] = (field.relatedModel_!.isEmbedded, field.isList, field.isRequired) match {
    case (true, _, _)          => None
    case (false, true, _)      => whereInputField(field, name = "disconnect")
    case (false, false, false) => Some(InputField[Any]("disconnect", OptionInputType(BooleanType)))
    case (false, false, true)  => None
  }

  def nestedDeleteInputField(field: RelationField): Option[InputField[Any]] = (field.isList, field.isRequired) match {
    case (true, _)      => whereInputField(field, name = "delete")
    case (false, false) => Some(InputField[Any]("delete", OptionInputType(BooleanType)))
    case (false, true)  => None
  }

  def trueInputFlag(field: RelationField, name: String): Option[InputField[Any]] = {
    val inputObjectType = inputObjectTypeForWhereUnique(field.relatedModel_!)
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any](name, x))
  }

  def whereInputField(field: RelationField, name: String): Option[InputField[Any]] = {
    val inputObjectType = inputObjectTypeForWhereUnique(field.relatedModel_!)
    generateInputType(inputObjectType, field.isList).map(x => InputField[Any](name, x))
  }

  private def generateInputType(input: Option[InputObjectType[Any]], isList: Boolean) = isList match {
    case true  => input.map(x => OptionInputType(ListInputType(x)))
    case false => input.map(x => OptionInputType(x))
  }
}

object FieldToInputTypeMapper {
  def mapForCreateCase(field: ScalarField): InputType[Any] = field.isRequired && field.defaultValue.isEmpty match {
    case true if field.isId =>
      (field.behaviour, field.typeIdentifier) match {
        case (Some(IdBehaviour(IdStrategy.Auto, _)) | None, TypeIdentifier.UUID | TypeIdentifier.Cuid) => SchemaBuilderUtils.mapToOptionalInputType(field)
        case (Some(IdBehaviour(IdStrategy.None, _)), TypeIdentifier.UUID | TypeIdentifier.Cuid)        => SchemaBuilderUtils.mapToRequiredInputType(field)
        case _                                                                                         => sys.error("Should not happen.")
      }
    case true  => SchemaBuilderUtils.mapToRequiredInputType(field)
    case false => SchemaBuilderUtils.mapToOptionalInputType(field)
  }
}
