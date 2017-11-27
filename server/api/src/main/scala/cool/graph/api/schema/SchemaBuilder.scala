package cool.graph.api.schema

import java.util.concurrent.TimeUnit

import akka.actor.ActorSystem
import cool.graph.api.database.{DataItem, DataResolver}
import cool.graph.api.database.DeferredTypes.{ManyModelDeferred, RelayConnectionOutputType, SimpleConnectionOutputType}
import cool.graph.shared.models.{Model, Project}
import org.atteo.evo.inflector.English
import sangria.schema._
import slick.jdbc.MySQLProfile.backend.DatabaseDef

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration

case class ApiUserContext(clientId: String)

trait SchemaBuilder {
  def apply(userContext: ApiUserContext, project: Project): Schema[ApiUserContext, Unit]
}

object SchemaBuilder {
  def apply()(implicit system: ActorSystem): SchemaBuilder = new SchemaBuilder {
    override def apply(userContext: ApiUserContext, project: Project) = SchemaBuilderImpl(userContext, project).build()
  }
}

case class SchemaBuilderImpl(
    userContext: ApiUserContext,
    project: Project
)(implicit system: ActorSystem) {
  import system.dispatcher

  val objectTypeBuilder = new ObjectTypeBuilder(project = project)
  val objectTypes       = objectTypeBuilder.modelObjectTypes
  val pluralsCache      = new PluralsCache

  def build(): Schema[ApiUserContext, Unit] = {
    val query        = buildQuery()
    val mutation     = buildMutation()
    val subscription = buildSubscription()

    Schema(
      query = query,
      mutation = mutation,
      subscription = subscription,
      validationRules = SchemaValidationRule.empty
    )
  }

  def buildQuery(): ObjectType[ApiUserContext, Unit] = {
//    val fields = {
//      ifFeatureFlag(generateGetAll, includedModels.map(getAllItemsField)) ++
//        ifFeatureFlag(generateGetAllMeta, includedModels.flatMap(getAllItemsMetaField)) ++
//        ifFeatureFlag(generateGetSingle, includedModels.map(getSingleItemField)) ++
//        ifFeatureFlag(generateCustomQueryFields, project.activeCustomQueryFunctions.map(getCustomResolverField)) ++
//        userField.toList :+ nodeField
//    }
//
//    ObjectType("Query", fields)

    val fields = project.models.map(getAllItemsField)

    ObjectType("Query", fields)
  }

  def buildMutation(): Option[ObjectType[ApiUserContext, Unit]] = {
//    val oneRelations                     = apiMatrix.filterRelations(project.getOneRelations)
//    val oneRelationsWithoutRequiredField = apiMatrix.filterNonRequiredRelations(oneRelations)
//
//    val manyRelations                     = apiMatrix.filterRelations(project.getManyRelations)
//    val manyRelationsWithoutRequiredField = apiMatrix.filterNonRequiredRelations(manyRelations)
//
//    val mutationFields: List[Field[UserContext, Unit]] = {
//      ifFeatureFlag(generateCreate, includedModels.filter(_.name != "User").map(getCreateItemField), measurementName = "CREATE") ++
//        ifFeatureFlag(generateUpdate, includedModels.map(getUpdateItemField), measurementName = "UPDATE") ++
//        ifFeatureFlag(generateUpdateOrCreate, includedModels.map(getUpdateOrCreateItemField), measurementName = "UPDATE_OR_CREATE") ++
//        ifFeatureFlag(generateDelete, includedModels.map(getDeleteItemField)) ++
//        ifFeatureFlag(generateSetRelation, oneRelations.map(getSetRelationField)) ++
//        ifFeatureFlag(generateUnsetRelation, oneRelationsWithoutRequiredField.map(getUnsetRelationField)) ++
//        ifFeatureFlag(generateAddToRelation, manyRelations.map(getAddToRelationField)) ++
//        ifFeatureFlag(generateRemoveFromRelation, manyRelationsWithoutRequiredField.map(getRemoveFromRelationField)) ++
//        ifFeatureFlag(generateIntegrationFields, getIntegrationFields) ++
//        ifFeatureFlag(generateCustomMutationFields, project.activeCustomMutationFunctions.map(getCustomResolverField))
//    }
//
//    if (mutationFields.isEmpty) None
//    else Some(ObjectType("Mutation", mutationFields))

    None
  }

  def buildSubscription(): Option[ObjectType[ApiUserContext, Unit]] = {
//    val subscriptionFields = { ifFeatureFlag(generateCreate, includedModels.map(getSubscriptionField)) }
//
//    if (subscriptionFields.isEmpty) None
//    else Some(ObjectType("Subscription", subscriptionFields))

    None
  }

  def getAllItemsField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"all${pluralsCache.pluralName(model)}",
      fieldType = ListType(objectTypes(model.name)),
      arguments = objectTypeBuilder.mapToListConnectionArguments(model),
      resolve = (ctx) => {
        val arguments = objectTypeBuilder.extractQueryArgumentsFromContext(model, ctx)

        DeferredValue(ManyModelDeferred(model, arguments)).map(_.toNodes)
      }
    )
  }

  def testField(): Field[ApiUserContext, Unit] = {
    Field(
      "viewer",
      fieldType = StringType,
      resolve = _ => akka.pattern.after(FiniteDuration(500, TimeUnit.MILLISECONDS), system.scheduler)(Future.successful("YES")) // "test"
    )
  }

}

class PluralsCache {
  private val cache = mutable.Map.empty[Model, String]

  def pluralName(model: Model): String = cache.getOrElseUpdate(
    key = model,
    op = English.plural(model.name).capitalize
  )
}
