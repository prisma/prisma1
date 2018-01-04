package cool.graph.api.schema

import cool.graph.cache.Cache
import cool.graph.shared.models.{Field, Model, Project, Relation}
import sangria.schema.{InputField, InputObjectType, InputType, ListInputType, OptionInputType}

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
    val inputObjectTypeName = omitRelation match {
      case None =>
        s"${model.name}CreateInput"

      case Some(relation) =>
        val field = relation.getField_!(project.schema, model)
        s"${model.name}CreateWithout${field.name.capitalize}Input"
    }

    val fields = computeScalarInputFieldsForCreate(model) ++ computeRelationalInputFieldsForCreate(model, omitRelation)

    if (fields.nonEmpty) {
      Some(
        InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () => { fields }
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
          fieldsFn = () => { fields }
        )
      )
    } else {
      None
    }
  }

  protected def computeInputObjectTypeForNestedUpdate(model: Model, omitRelation: Relation): Option[InputObjectType[Any]] = {
    val field           = omitRelation.getField_!(project.schema, model)
    val updateDataInput = computeInputObjectTypeForNestedUpdateData(model, omitRelation)

    computeInputObjectTypeForWhereUnique(model).map { whereArg =>
      InputObjectType[Any](
        name = s"${model.name}UpdateWithout${field.name.capitalize}Input",
        fieldsFn = () => {
          List(
            InputField[Any]("where", whereArg),
            InputField[Any]("data", updateDataInput)
          )
        }
      )
    }
  }

  protected def computeInputObjectTypeForNestedUpdateData(model: Model, omitRelation: Relation): InputObjectType[Any] = {
    val field = omitRelation.getField_!(project.schema, model)

    InputObjectType[Any](
      name = s"${model.name}UpdateWithout${field.name.capitalize}DataInput",
      fieldsFn = () => {
        computeScalarInputFieldsForUpdate(model) ++ computeRelationalInputFieldsForUpdate(model, omitRelation = Some(omitRelation))
      }
    )
  }

  protected def computeInputObjectTypeForNestedUpsert(model: Model, omitRelation: Relation): Option[InputObjectType[Any]] = {
    val field = omitRelation.getField_!(project.schema, model)

    computeInputObjectTypeForWhereUnique(model).flatMap { whereArg =>
      computeInputObjectTypeForCreate(model, Some(omitRelation)).map { createArg =>
        InputObjectType[Any](
          name = s"${model.name}UpsertWithout${field.name.capitalize}Input",
          fieldsFn = () => {
            List(
              InputField[Any]("where", whereArg),
              InputField[Any]("update", computeInputObjectTypeForNestedUpdateData(model, omitRelation)),
              InputField[Any]("create", createArg)
            )
          }
        )
      }
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
    val nonListFields = model.scalarFields.filter(!_.isList).map { field =>
      InputField(field.name, mapToInputType(field))
    }

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

  private def computeNonListScalarInputFields(model: Model, mapToInputType: Field => InputType[Any]): List[InputField[Any]] = {
    model.scalarFields.filter(!_.isList).map { field =>
      InputField(field.name, mapToInputType(field))
    }
  }

  private def computeRelationalInputFieldsForUpdate(model: Model, omitRelation: Option[Relation]): List[InputField[Any]] = {
    model.relationFields.flatMap { field =>
      val subModel              = field.relatedModel_!(project.schema)
      val relatedField          = field.relatedFieldEager(project.schema)
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      val inputObjectTypeName = if (field.isList) {
        s"${subModel.name}UpdateManyWithout${relatedField.name.capitalize}Input"
      } else {
        s"${subModel.name}UpdateOneWithout${relatedField.name.capitalize}Input"
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
      val relatedField          = field.relatedFieldEager(project.schema)
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      val inputObjectTypeName = if (field.isList) {
        s"${subModel.name}CreateManyWithout${relatedField.name.capitalize}Input"
      } else {
        s"${subModel.name}CreateOneWithout${relatedField.name.capitalize}Input"
      }

      if (relationMustBeOmitted) {
        None
      } else {
        val inputObjectType = InputObjectType[Any](
          name = inputObjectTypeName,
          fieldsFn = () => nestedCreateInputField(field).toList ++ nestedConnectInputField(field)
        )
        val possiblyRequired = if (field.isRequired) { inputObjectType } else { OptionInputType(inputObjectType) }

        Some(InputField[Any](field.name, possiblyRequired))
      }
    }
  }

  def nestedUpdateInputField(field: Field): Option[InputField[Any]] = {
    val subModel = field.relatedModel_!(project.schema)
    val relation = field.relation.get
    val inputType = if (field.isList) {
      computeInputObjectTypeForNestedUpdate(subModel, omitRelation = relation).map(x => OptionInputType(ListInputType(x)))
    } else {
      computeInputObjectTypeForNestedUpdate(subModel, omitRelation = relation).map(x => OptionInputType(x))
    }

    inputType.map(x => InputField[Any]("update", x))
  }

  def nestedCreateInputField(field: Field): Option[InputField[Any]] = {
    val subModel = field.relatedModel_!(project.schema)
    val relation = field.relation.get
    val inputType = if (field.isList) {
      inputObjectTypeForCreate(subModel, Some(relation)).map(x => OptionInputType(ListInputType(x)))
    } else {
      inputObjectTypeForCreate(subModel, Some(relation)).map(x => OptionInputType(x))
    }

    inputType.map(x => InputField[Any]("create", x))
  }

  def nestedUpsertInputField(field: Field): Option[InputField[Any]] = {
    val subModel = field.relatedModel_!(project.schema)
    val relation = field.relation.get
    val inputType = if (field.isList) {
      computeInputObjectTypeForNestedUpsert(subModel, relation).map(x => OptionInputType(ListInputType(x)))
    } else {
      computeInputObjectTypeForNestedUpsert(subModel, relation).map(x => OptionInputType(x))
    }

    inputType.map(x => InputField[Any]("upsert", x))
  }

  def nestedConnectInputField(field: Field): Option[InputField[Any]]    = whereInputField(field, name = "connect")
  def nestedDisconnectInputField(field: Field): Option[InputField[Any]] = whereInputField(field, name = "disconnect")
  def nestedDeleteInputField(field: Field): Option[InputField[Any]]     = whereInputField(field, name = "delete")

  def whereInputField(field: Field, name: String): Option[InputField[Any]] = {
    val subModel = field.relatedModel_!(project.schema)

    inputObjectTypeForWhereUnique(subModel).map { inputObjectType =>
      val inputType = if (field.isList) {
        OptionInputType(ListInputType(inputObjectType))
      } else {
        OptionInputType(inputObjectType)
      }

      InputField[Any](name, inputType)
    }
  }
}

object FieldToInputTypeMapper {
  def mapForCreateCase(field: Field): InputType[Any] = field.isRequired && field.defaultValue.isEmpty match {
    case true  => SchemaBuilderUtils.mapToRequiredInputType(field)
    case false => SchemaBuilderUtils.mapToOptionalInputType(field)
  }
}
