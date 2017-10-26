package cool.graph.client.schema

import java.lang.{StringBuilder => JStringBuilder}

import com.github.benmanes.caffeine.cache.{Cache, Caffeine}
import cool.graph.client.SchemaBuilderUtils
import cool.graph.shared.models.{Field, Model, Project, Relation}
import cool.graph.{ArgumentSchema, SchemaArgument}
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

case class InputTypesBuilder(project: Project, argumentSchema: ArgumentSchema) {
  import CaffeineCacheExtensions._

  val caffeineCache: Cache[String, Object] = Caffeine.newBuilder().build[String, Object]()
  private val oneRelationIdFieldType       = OptionInputType(IDType)
  private val manyRelationIdsFieldType     = OptionInputType(ListInputType(IDType))

  def getSangriaArgumentsForCreate(model: Model): List[Argument[Any]] = {
    getSangriaArguments(inputObjectType = cachedInputObjectTypeForCreate(model), arguments = cachedSchemaArgumentsForCreate(model))
  }

  def getSangriaArgumentsForUpdate(model: Model): List[Argument[Any]] = {
    getSangriaArguments(inputObjectType = cachedInputObjectTypeForUpdate(model), arguments = cachedSchemaArgumentsForUpdate(model))
  }

  def getSangriaArgumentsForUpdateOrCreate(model: Model): List[Argument[Any]] = {
    getSangriaArguments(inputObjectType = cachedInputObjectTypeForUpdateOrCreate(model), arguments = cachedSchemaArgumentsForUpdateOrCreate(model))
  }

  private def getSangriaArguments(inputObjectType: => InputObjectType[Any], arguments: => List[SchemaArgument]): List[Argument[Any]] = {
    argumentSchema.convertSchemaArgumentsToSangriaArguments(inputObjectType.name, arguments)
  }

  // UPDATE_OR_CREATE CACHES
  private def cachedInputObjectTypeForUpdateOrCreate(model: Model): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForUpdateOrCreate", model)) {
      InputObjectType[Any](
        name = s"UpdateOrCreate${model.name}",
        fieldsFn = () => {
          val updateField = InputField("update", cachedInputObjectTypeForUpdate(model))
          val createField = InputField("create", cachedInputObjectTypeForCreate(model))

          if (cachedInputObjectTypeForCreate(model).fields.isEmpty) {
            List(updateField)
          } else {

            List(updateField, createField)
          }
        }
      )
    }
  }

  private def cachedSchemaArgumentsForUpdateOrCreate(model: Model): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedSchemaArgumentsForUpdateOrCreate", model)) {
      val createInputType = cachedInputObjectTypeForCreate(model)
      val updateArgument  = SchemaArgument("update", cachedInputObjectTypeForUpdate(model))
      val createArgument  = SchemaArgument("create", createInputType)

      if (createInputType.fields.isEmpty) {
        List(updateArgument)
      } else {
        List(updateArgument, createArgument)
      }

    }
  }

  // CREATE CACHES
  private def cachedInputObjectTypeForCreate(model: Model, omitRelation: Option[Relation] = None): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForCreate", model, omitRelation)) {
      val inputObjectTypeName = omitRelation match {
        case None =>
          s"Create${model.name}"

        case Some(relation) =>
          val otherModel = relation.getOtherModel_!(project, model)
          val otherField = relation.getOtherField_!(project, model)

          s"${otherModel.name}${otherField.name}${model.name}"
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
      computeScalarSchemaArgumentsForCreate(model) ++ cachedRelationalSchemaArguments(model, omitRelation = omitRelation)
    }
  }

  // UPDATE CACHES
  private def cachedInputObjectTypeForUpdate(model: Model): InputObjectType[Any] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedInputObjectTypeForUpdate", model)) {
      InputObjectType[Any](
        name = s"Update${model.name}",
        fieldsFn = () => {
          val schemaArguments = cachedSchemaArgumentsForUpdate(model)
          schemaArguments.map(_.asSangriaInputField)
        }
      )
    }
  }

  private def cachedSchemaArgumentsForUpdate(model: Model): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedSchemaArgumentsForUpdate", model)) {
      computeScalarSchemaArgumentsForUpdate(model) ++ cachedRelationalSchemaArguments(model, omitRelation = None)
    }
  }

  // RELATIONAL CACHE

  def cachedRelationalSchemaArguments(model: Model, omitRelation: Option[Relation]): List[SchemaArgument] = {
    caffeineCache.getOrElseUpdate(cacheKey("cachedRelationalSchemaArguments", model, omitRelation)) {
      computeRelationalSchemaArguments(model, omitRelation)
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

  def computeScalarSchemaArgumentsForCreate(model: Model): List[SchemaArgument] = {
    val filteredModel = model.filterFields(_.isWritable)
    computeScalarSchemaArguments(filteredModel, FieldToInputTypeMapper.mapForCreateCase)
  }

  def computeScalarSchemaArgumentsForUpdate(model: Model): List[SchemaArgument] = {
    val filteredModel = model.filterFields(f => f.isWritable || f.name == "id")
    computeScalarSchemaArguments(filteredModel, FieldToInputTypeMapper.mapForUpdateCase)
  }

  private def computeScalarSchemaArguments(model: Model, mapToInputType: Field => InputType[Any]): List[SchemaArgument] = {
    model.scalarFields.map { field =>
      SchemaArgument(field.name, mapToInputType(field), field.description, field)
    }
  }

  private def computeRelationalSchemaArguments(model: Model, omitRelation: Option[Relation]): List[SchemaArgument] = {
    val oneRelationArguments = model.singleRelationFields.flatMap { field =>
      val subModel              = field.relatedModel_!(project)
      val relation              = field.relation.get
      val relationMustBeOmitted = omitRelation.exists(rel => field.isRelationWithId(rel.id))

      val idArg = schemaArgumentWithName(
        field = field,
        name = field.name + SchemaBuilderConstants.idSuffix,
        inputType = oneRelationIdFieldType
      )

      if (relationMustBeOmitted) {
        List.empty
      } else if (project.hasEnabledAuthProvider && subModel.isUserModel) {
        List(idArg)
      } else if (!subModel.fields.exists(f => f.isWritable && !f.relation.exists(_ => !f.isList && f.isRelationWithId(relation.id)))) {
        List(idArg)
      } else {
        val inputObjectType = OptionInputType(cachedInputObjectTypeForCreate(subModel, omitRelation = Some(relation)))
        val complexArg      = schemaArgument(field = field, inputType = inputObjectType)
        List(idArg, complexArg)
      }
    }

    val manyRelationArguments = model.listRelationFields.flatMap { field =>
      val subModel = field.relatedModel_!(project)
      val relation = field.relation.get
      val idsArg = schemaArgumentWithName(
        field = field,
        name = field.name + SchemaBuilderConstants.idListSuffix,
        inputType = manyRelationIdsFieldType
      )

      if (project.hasEnabledAuthProvider && subModel.isUserModel) {
        List(idsArg)
      } else if (!subModel.fields.exists(f => f.isWritable && !f.relation.exists(rel => !f.isList && f.isRelationWithId(relation.id)))) {
        List(idsArg)
      } else {
        val inputObjectType = cachedInputObjectTypeForCreate(subModel, omitRelation = Some(relation))
        val complexArg      = schemaArgument(field, inputType = OptionInputType(ListInputType(inputObjectType)))
        List(idsArg, complexArg)
      }
    }
    oneRelationArguments ++ manyRelationArguments
  }

  private def schemaArgument(field: Field, inputType: InputType[Any]): SchemaArgument = {
    schemaArgumentWithName(field = field, name = field.name, inputType = inputType)
  }

  private def schemaArgumentWithName(field: Field, name: String, inputType: InputType[Any]): SchemaArgument = {
    SchemaArgument(name = name, inputType = inputType, description = field.description, field = field)
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
