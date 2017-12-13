package cool.graph.api.schema

import java.lang.{StringBuilder => JStringBuilder}

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import cool.graph.api.mutations.MutationTypes.ArgumentValue
import cool.graph.shared.models.{Field, Model, Project, Relation}
import cool.graph.util.coolSangria.FromInputImplicit
import sangria.schema.{InputObjectType, _}

object CaffeineCacheExtensions {
  implicit class GetOrElseUpdateExtension[K](val cache: Cache[K, Object]) extends AnyVal {
    def getOrElseUpdate[T <: AnyRef](cacheKey: K)(fn: => T): T = {
      val cacheEntry = cache.getIfPresent(cacheKey)
      if (cacheEntry != null) {
        cacheEntry.asInstanceOf[T]
      } else {
        val result = fn
        cache.put(cacheKey, result)
        result
      }
    }
  }
}

case class InputTypesBuilder(project: Project) {
  import CaffeineCacheExtensions._

  val caffeineCache: Cache[String, Object] = Caffeine.newBuilder().build[String, Object]()
  private val oneRelationIdFieldType       = OptionInputType(IDType)
  private val manyRelationIdsFieldType     = OptionInputType(ListInputType(IDType))

  implicit val anyFromInput = FromInputImplicit.CoercedResultMarshaller

  def getSangriaArgumentsForCreate(model: Model): List[Argument[Any]] = {
    val inputObjectType = cachedInputObjectTypeForCreate(model)
    List(Argument[Any]("data", inputObjectType))
  }

  def getSangriaArgumentsForUpdate(model: Model): List[Argument[Any]] = {
    val inputObjectType = cachedInputObjectTypeForUpdate(model)
    List(Argument[Any]("data", inputObjectType), getWhereArgument(model))
  }

  def getSangriaArgumentsForUpdateOrCreate(model: Model): List[Argument[Any]] = {
    List(
      Argument[Any]("create", cachedInputObjectTypeForCreate(model)),
      Argument[Any]("update", cachedInputObjectTypeForUpdate(model)),
      Argument[Any]("where", ???)
    )
  }

  def getSangriaArgumentsForDelete(model: Model): List[Argument[Any]] = {
    List(getWhereArgument(model))
  }

  private def getWhereArgument(model: Model) = {
    Argument[Any](
      name = "where",
      argumentType = InputObjectType(
        name = s"${model.name}WhereUniqueInput",
        fields = model.fields.filter(_.isUnique).map(field => InputField(name = field.name, fieldType = SchemaBuilderUtils.mapToOptionalInputType(field)))
      )
    )
  }

  // CACHES
  private def cachedInputObjectTypeForCreate(model: Model, omitRelation: Option[Relation] = None): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForCreate", model, omitRelation)) {
      computeInputObjectTypeForCreate(model, omitRelation)
    }
  }

  private def cachedInputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForUpdate", model)) {
      computenInputObjectTypeForUpdate(model)
    }
  }

  private def cacheKey(name: String, model: Model, relation: Option[Relation] = None): String = {
    val sb = new JStringBuilder()
    sb.append(name)
    sb.append(model.id)
    sb.append(relation.orNull)
    sb.toString
  }

  // COMPUTE METHODS

  private def computeInputObjectTypeForCreate(model: Model, omitRelation: Option[Relation]): InputObjectType[Any] = {
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

  private def computenInputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
    InputObjectType[Any](
      name = s"${model.name}UpdateInput",
      fieldsFn = () => {
        val schemaArguments = computeScalarSchemaArgumentsForUpdate(model) ++
          computeRelationalSchemaArguments(model, omitRelation = None, operation = "Update")

        schemaArguments.map(_.asSangriaInputField)
      }
    )
  }

  private def computeByArguments(model: Model): List[SchemaArgument] = {
    model.fields.filter(_.isUnique).map { field =>
      SchemaArgument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), field.description)
    }
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
              SchemaArgument("create", OptionInputType(ListInputType(cachedInputObjectTypeForCreate(subModel, Some(relation))))).asSangriaInputField
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
              SchemaArgument("create", OptionInputType(cachedInputObjectTypeForCreate(subModel, Some(relation)))).asSangriaInputField
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
