package cool.graph.api.schema

import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.cache.Cache
import cool.graph.shared.models.{Field, Model, Project, Relation}
import cool.graph.util.coolSangria.FromInputImplicit
import sangria.schema.{Args, InputField, InputObjectType, InputType, ListInputType, OptionInputType}

trait InputTypesBuilder {
  def inputObjectTypeForCreate(model: Model, omitRelation: Option[Relation] = None): InputObjectType[Any]

  def inputObjectTypeForUpdate(model: Model): InputObjectType[Any]

  def inputObjectTypeForWhere(model: Model): InputObjectType[Any]
}

case class CachedInputTypesBuilder(project: Project) extends UncachedInputTypesBuilder(project) {
  import java.lang.{StringBuilder => JStringBuilder}

  val cache = Cache.unbounded[String, InputObjectType[Any]]()

  override def inputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): InputObjectType[Any] = {
    cache.getOrUpdate(cacheKey("cachedInputObjectTypeForCreate", model, omitRelation), { () =>
      computeInputObjectTypeForCreate(model, omitRelation)
    })
  }

  override def inputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
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
  override def inputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): InputObjectType[Any] = {
    computeInputObjectTypeForCreate(model, omitRelation)
  }

  override def inputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
    computeInputObjectTypeForUpdate(model)
  }

  override def inputObjectTypeForWhere(model: Model): InputObjectType[Any] = {
    computeInputObjectTypeForWhere(model)
  }

  protected def computeInputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): InputObjectType[Any] = {
    val inputObjectTypeName = omitRelation match {
      case None =>
        s"${model.name}CreateInput"

      case Some(relation) =>
        val field = relation.getField_!(project, model)
        s"${model.name}CreateWithout${field.name.capitalize}Input"
    }

    InputObjectType[Any](
      name = inputObjectTypeName,
      fieldsFn = () => {
        val schemaArguments = computeScalarSchemaArgumentsForCreate(model) ++ computeRelationalSchemaArguments(model, omitRelation, operation = "Create")
        schemaArguments.map(_.asSangriaInputField)
      }
    )
  }

  protected def computeInputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
    InputObjectType[Any](
      name = s"${model.name}UpdateInput",
      fieldsFn = () => {
        val schemaArguments = computeScalarSchemaArgumentsForUpdate(model) ++
          computeRelationalSchemaArguments(model, omitRelation = None, operation = "Update")

        schemaArguments.map(_.asSangriaInputField)
      }
    )
  }

  protected def computeInputObjectTypeForWhere(model: Model): InputObjectType[Any] = {
    InputObjectType[Any](
      name = s"${model.name}WhereUniqueInput",
      fields = model.fields.filter(_.isUnique).map(field => InputField(name = field.name, fieldType = SchemaBuilderUtils.mapToOptionalInputType(field)))
    )
  }

  private def computeScalarSchemaArgumentsForCreate(model: Model): List[SchemaArgument] = {
    val filteredModel = model.filterFields(_.isWritable)
    computeScalarSchemaArguments(filteredModel, FieldToInputTypeMapper.mapForCreateCase)
  }

  private def computeScalarSchemaArgumentsForUpdate(model: Model): List[SchemaArgument] = {
    val filteredModel = model.filterFields(f => f.isWritable)
    computeScalarSchemaArguments(filteredModel, FieldToInputTypeMapper.mapForUpdateCase)
  }

  private def computeScalarSchemaArguments(model: Model, mapToInputType: Field => InputType[Any]): List[SchemaArgument] = {
    model.scalarFields.map { field =>
      SchemaArgument(field.name, mapToInputType(field), field.description)
    }
  }

  private def computeRelationalSchemaArguments(model: Model, omitRelation: Option[Relation], operation: String): List[SchemaArgument] = {
    val manyRelationArguments = model.listRelationFields.flatMap { field =>
      val subModel              = field.relatedModel_!(project)
      val relation              = field.relation.get
      val relatedField          = field.relatedFieldEager(project)
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      if (relationMustBeOmitted) {
        None
      } else {
        val inputObjectType = InputObjectType[Any](
          name = s"${subModel.name}${operation}ManyWithout${relatedField.name.capitalize}Input",
          fieldsFn = () => {
            List(
              SchemaArgument("create", OptionInputType(ListInputType(inputObjectTypeForCreate(subModel, Some(relation))))).asSangriaInputField,
              SchemaArgument("connect", OptionInputType(ListInputType(inputObjectTypeForWhere(subModel)))).asSangriaInputField
            )
          }
        )
        Some(SchemaArgument(field.name, OptionInputType(inputObjectType), field.description))
      }
    }
    val singleRelationArguments = model.singleRelationFields.flatMap { field =>
      val subModel              = field.relatedModel_!(project)
      val relation              = field.relation.get
      val relatedField          = field.relatedFieldEager(project)
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      if (relationMustBeOmitted) {
        None
      } else {
        val inputObjectType = InputObjectType[Any](
          name = s"${subModel.name}${operation}OneWithout${relatedField.name.capitalize}Input",
          fieldsFn = () => {
            List(
              SchemaArgument("create", OptionInputType(inputObjectTypeForCreate(subModel, Some(relation)))).asSangriaInputField,
              SchemaArgument("connect", OptionInputType(inputObjectTypeForWhere(subModel))).asSangriaInputField
            )
          }
        )
        Some(SchemaArgument(field.name, OptionInputType(inputObjectType), field.description))
      }
    }
    manyRelationArguments ++ singleRelationArguments
  }
}

object FieldToInputTypeMapper {
  def mapForCreateCase(field: Field): InputType[Any] = field.isRequired && field.defaultValue.isEmpty match {
    case true  => SchemaBuilderUtils.mapToRequiredInputType(field)
    case false => SchemaBuilderUtils.mapToOptionalInputType(field)
  }

  def mapForUpdateCase(field: Field): InputType[Any] = field.name match {
    case "id" => SchemaBuilderUtils.mapToRequiredInputType(field)
    case _    => SchemaBuilderUtils.mapToOptionalInputType(field)
  }
}

case class SchemaArgument(name: String, inputType: InputType[Any], description: Option[String] = None) {

  lazy val asSangriaInputField = InputField(name, inputType, description.getOrElse(""))
  //lazy val asSangriaArgument   = Argument.createWithoutDefault(name, inputType, description)
}

object SchemaArgument {

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  def extractArgumentValues(args: Args, argumentDefinitions: List[SchemaArgument]): List[ArgumentValue] = {
    argumentDefinitions
      .filter(a => args.raw.contains(a.name))
      .map { a =>
        val value = args.raw.get(a.name) match {
          case Some(Some(v)) => v
          case Some(v)       => v
          case v             => v
        }
        ArgumentValue(a.name, value)
      }
  }
}
