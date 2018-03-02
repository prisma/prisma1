package com.prisma.api.schema

import com.prisma.cache.Cache
import com.prisma.shared.models.{Field, Model, Project, Relation}
import sangria.ast.BooleanValue
import sangria.schema._

trait InputTypesBuilder {
  def inputObjectTypeForCreate(model: Model, omitRelation: Option[Relation] = None): Option[InputObjectType[Any]]

  def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]]

  def inputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]]

  def inputObjectTypeForWhere(model: Model): InputObjectType[Any]
}

case class CachedInputTypesBuilder(project: Project) extends UncachedInputTypesBuilder(project) {
  import java.lang.{StringBuilder => JStringBuilder}

  val cache = Cache.unbounded[String, Option[InputObjectType[Any]]]()

  override def inputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForCreate", model, omitRelation), { () =>
      computeInputObjectTypeForCreate(model, omitRelation)
    })
  }

  override def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForUpdate", model), { () =>
      computeInputObjectTypeForUpdate(model)
    })
  }

  private def cacheKey(name: String, model: Model, relation: Option[Relation] = None): String = {
    val sb = new JStringBuilder()

    sb.append(name)
    sb.append(model.id)
    sb.append(relation.orNull)
    sb.toString
  }
}

abstract class UncachedInputTypesBuilder(project: Project) extends InputTypesBuilder {
  override def inputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForCreate(model, omitRelation)
  }

  override def inputObjectTypeForUpdate(model: Model): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForUpdate(model)
  }

  override def inputObjectTypeForWhereUnique(model: Model): Option[InputObjectType[Any]] = {
    computeInputObjectTypeForWhereUnique(model)
  }

  override def inputObjectTypeForWhere(model: Model): InputObjectType[Any] = {
    computeInputObjectTypeForWhere(model)
  }

  protected def computeInputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): Option[InputObjectType[Any]] = {
    val inputObjectTypeName = omitRelation.flatMap(_.getField(project.schema, model)) match {
      case None        => s"${model.name}CreateInput"
      case Some(field) => s"${model.name}CreateWithout${field.name.capitalize}Input"
    }

    val fields = computeScalarInputFieldsForCreate(model) ++ computeRelationalInputFieldsForCreate(model, omitRelation)

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
    val fields = computeScalarInputFieldsForUpdate(model) ++ computeRelationalInputFieldsForUpdate(model, omitRelation = None)

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

  protected def computeInputObjectTypeForNestedUpdate(model: Model, omitRelation: Relation, isList: Boolean): Option[InputObjectType[Any]] = {
    val updateDataInput = computeInputObjectTypeForNestedUpdateData(model, omitRelation)

    if (updateDataInput.isDefined) {
      if (isList) {
        for {
          whereArg <- computeInputObjectTypeForWhereUnique(model)
        } yield {
          val typeName = omitRelation.getField(project.schema, model) match {
            case Some(field) => s"${model.name}UpdateWithWhereUniqueWithout${field.name.capitalize}Input"
            case None        => s"${model.name}UpdateWithWhereUniqueNestedInput"
          }

          InputObjectType[Any](
            name = typeName,
            fieldsFn = () => {
              List(
                InputField[Any]("where", whereArg),
                InputField[Any]("data", updateDataInput.get)
              )
            }
          )
        }
      } else {
        Some(updateDataInput.get)
      }
    } else { None }

  }

  protected def computeInputObjectTypeForNestedUpdateData(model: Model, omitRelation: Relation): Option[InputObjectType[Any]] = {

    val fields = computeScalarInputFieldsForUpdate(model) ++ computeRelationalInputFieldsForUpdate(model, omitRelation = Some(omitRelation))

    if (fields.nonEmpty) {
      val typeName = omitRelation.getField(project.schema, model) match {
        case Some(field) => s"${model.name}UpdateWithout${field.name.capitalize}DataInput"
        case None        => s"${model.name}UpdateDataInput"
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

  protected def computeInputObjectTypeForNestedUpsert(model: Model, omitRelation: Relation, isList: Boolean): Option[InputObjectType[Any]] = {
    val updateDataInput = computeInputObjectTypeForNestedUpdateData(model, omitRelation)

    if (updateDataInput.isDefined) {
      if (isList) {
        for {
          whereArg  <- computeInputObjectTypeForWhereUnique(model)
          createArg <- computeInputObjectTypeForCreate(model, Some(omitRelation))
        } yield {
          val typeName = omitRelation.getField(project.schema, model) match {
            case Some(field) => s"${model.name}UpsertWithWhereUniqueWithout${field.name.capitalize}Input"
            case None        => s"${model.name}UpsertWithWhereUniqueNestedInput"
          }

          InputObjectType[Any](
            name = typeName,
            fieldsFn = () => {
              List(
                InputField[Any]("where", whereArg),
                InputField[Any]("update", updateDataInput.get),
                InputField[Any]("create", createArg)
              )
            }
          )
        }
      } else {
        for {
          createArg <- computeInputObjectTypeForCreate(model, Some(omitRelation))
        } yield {
          val typeName = omitRelation.getField(project.schema, model) match {
            case Some(field) => s"${model.name}UpsertWithout${field.name.capitalize}Input"
            case None        => s"${model.name}UpsertNestedInput"
          }

          InputObjectType[Any](
            name = typeName,
            fieldsFn = () => {
              List(
                InputField[Any]("update", updateDataInput.get),
                InputField[Any]("create", createArg)
              )
            }
          )
        }
      }
    } else { None }
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

  private def computeRelationalInputFieldsForUpdate(model: Model, omitRelation: Option[Relation]): List[InputField[Any]] = {
    model.relationFields.flatMap { field =>
      val subModel              = field.relatedModel_!(project.schema)
      val relatedField          = field.relatedField(project.schema)
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      val inputObjectTypeName = {
        val arityPart = if (field.isList) "Many" else "One"
        val withoutPart = relatedField match {
          case Some(field) => s"Without${field.name.capitalize}"
          case None        => ""
        }
        s"${subModel.name}Update${arityPart}${withoutPart}Input"
      }

      if (relationMustBeOmitted) {
        None
      } else {
        val inputObjectType = InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () =>
            nestedCreateInputField(field).toList ++
              nestedConnectInputField(field) ++
              nestedDisconnectInputField(field) ++
              nestedDeleteInputField(field) ++
              nestedUpdateInputField(field) ++
              nestedUpsertInputField(field)
        )
        Some(InputField[Any](field.name, OptionInputType(inputObjectType)))
      }
    }
  }

  private def computeRelationalInputFieldsForCreate(model: Model, omitRelation: Option[Relation]): List[InputField[Any]] = {
    model.relationFields.flatMap { field =>
      val subModel              = field.relatedModel_!(project.schema)
      val relatedField          = field.relatedField(project.schema)
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      val inputObjectTypeName = {
        val arityPart = if (field.isList) "Many" else "One"
        val withoutPart = relatedField match {
          case Some(field) => s"Without${field.name.capitalize}"
          case None        => ""
        }
        s"${subModel.name}Create${arityPart}${withoutPart}Input"
      }

      if (relationMustBeOmitted) {
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
    val subModel        = field.relatedModel_!(project.schema)
    val relation        = field.relation.get
    val inputObjectType = computeInputObjectTypeForNestedUpdate(subModel, omitRelation = relation, field.isList)

    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("update", x))
  }

  def nestedCreateInputField(field: Field): Option[InputField[Any]] = {
    val subModel        = field.relatedModel_!(project.schema)
    val relation        = field.relation.get
    val inputObjectType = inputObjectTypeForCreate(subModel, Some(relation))

    generateInputType(inputObjectType, field.isList).map(x => InputField[Any]("create", x))
  }

  def nestedUpsertInputField(field: Field): Option[InputField[Any]] = {
    val subModel        = field.relatedModel_!(project.schema)
    val relation        = field.relation.get
    val inputObjectType = computeInputObjectTypeForNestedUpsert(subModel, relation, field.isList)

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
    val subModel        = field.relatedModel_!(project.schema)
    val inputObjectType = inputObjectTypeForWhereUnique(subModel)

    generateInputType(inputObjectType, field.isList).map(x => InputField[Any](name, x))
  }

  def whereInputField(field: Field, name: String): Option[InputField[Any]] = {
    val subModel        = field.relatedModel_!(project.schema)
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
