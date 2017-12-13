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
    //getSangriaArguments(inputObjectType = cachedInputObjectTypeForCreate(model), arguments = cachedSchemaArgumentsForCreate(model))
    val inputObjectType = cachedInputObjectTypeForCreate(model)
    List(Argument[Any]("data", inputObjectType))
  }

  def getSangriaArgumentsForUpdate(model: Model): List[Argument[Any]] = {
    //getSangriaArguments(inputObjectType = cachedInputObjectTypeForUpdate(model), arguments = cachedSchemaArgumentsForUpdate(model))
    val inputObjectType = cachedInputObjectTypeForUpdate(model)
    List(Argument[Any]("data", inputObjectType))
  }

  def getSangriaArgumentsForUpdateOrCreate(model: Model): List[Argument[Any]] = {
    List(
      Argument[Any]("create", cachedInputObjectTypeForCreate(model)),
      Argument[Any]("update", cachedInputObjectTypeForUpdate(model)),
      Argument[Any]("where", ???)
    )
  }

  // CREATE CACHES
  private def cachedInputObjectTypeForCreate(model: Model, omitRelation: Option[Relation] = None): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForCreate", model, omitRelation)) {
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
          val schemaArguments = cachedSchemaArgumentsForCreate(model, omitRelation = omitRelation)
          schemaArguments.map(_.asSangriaInputField)
        }
      )
    }
  }

  private def cachedSchemaArgumentsForCreate(model: Model, omitRelation: Option[Relation] = None): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedSchemaArgumentsForCreate", model, omitRelation)) {
      computeScalarSchemaArgumentsForCreate(model) ++ cachedRelationalSchemaArgumentsForCreate(model, omitRelation = omitRelation)
    }
  }

  // UPDATE CACHES
  private def cachedInputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForUpdate", model)) {
      InputObjectType[Any](
        name = s"${model.name}UpdateInput",
        fieldsFn = () => {
          val schemaArguments = cachedSchemaArgumentsForUpdate(model)
          schemaArguments.map(_.asSangriaInputField)
        }
      )
    }
  }

  private def cachedSchemaArgumentsForUpdate(model: Model): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedSchemaArgumentsForUpdate", model)) {
      computeScalarSchemaArgumentsForUpdate(model) ++ cachedRelationalSchemaArgumentsForUpdate(model, omitRelation = None)
    }
  }

  // RELATIONAL CACHE

  def cachedRelationalSchemaArgumentsForCreate(model: Model, omitRelation: Option[Relation]): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedRelationalSchemaArgumentsForCreate", model, omitRelation)) {
      computeRelationalSchemaArguments(model, omitRelation, operation = "Create")
    }
  }

  def cachedRelationalSchemaArgumentsForUpdate(model: Model, omitRelation: Option[Relation]): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedRelationalSchemaArgumentsForUpdate", model, omitRelation)) {
      computeRelationalSchemaArguments(model, omitRelation, operation = "Update")
    }
  }

  // CACHE KEYS

  private def cacheKey(name: String, model: Model, relation: Option[Relation]): String = {
    val sb = new JStringBuilder()
    sb.append(name)
    sb.append(model.id)
    sb.append(relation.orNull)
    sb.toString
  }

  private def cacheKey(name: String, model: Model): String = {
    val sb = new JStringBuilder()
    sb.append(name)
    sb.append(model.id)
    sb.toString
  }

  // COMPUTE METHODS

  def computeByArguments(model: Model): List[SchemaArgument] = {
    model.fields.filter(_.isUnique).map { field =>
      SchemaArgument(field.name, SchemaBuilderUtils.mapToOptionalInputType(field), field.description)
    }
  }

  def computeScalarSchemaArgumentsForCreate(model: Model): List[SchemaArgument] = {
    val filteredModel = model.filterFields(_.isWritable)
    computeScalarSchemaArguments(filteredModel, FieldToInputTypeMapper.mapForCreateCase)
  }

  def computeScalarSchemaArgumentsForUpdate(model: Model): List[SchemaArgument] = {
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
