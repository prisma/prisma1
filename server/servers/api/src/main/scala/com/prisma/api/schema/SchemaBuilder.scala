package com.prisma.api.schema

import akka.actor.ActorSystem
import com.prisma.api.connector.{CoolArgs, DataItem}
import com.prisma.api.mutations._
import com.prisma.api.resolver.DeferredTypes.{ManyModelDeferred, OneDeferred}
import com.prisma.api.{ApiDependencies, ApiMetrics}
import com.prisma.shared.models.{Model, Project}
import org.atteo.evo.inflector.English
import sangria.relay._
import sangria.schema._

import scala.collection.mutable
import scala.concurrent.Future

case class ApiUserContext(clientId: String)

trait SchemaBuilder {
  def apply(project: Project): Schema[ApiUserContext, Unit]
}

object SchemaBuilder {
  def apply()(implicit system: ActorSystem, apiDependencies: ApiDependencies): SchemaBuilder = new SchemaBuilder {
    override def apply(project: Project) = SchemaBuilderImpl(project).build()
  }
}

case class SchemaBuilderImpl(
    project: Project
)(implicit apiDependencies: ApiDependencies, system: ActorSystem) {
  import system.dispatcher

  val argumentsBuilder                     = ArgumentsBuilder(project = project)
  val dataResolver                         = apiDependencies.dataResolver(project)
  val masterDataResolver                   = apiDependencies.masterDataResolver(project)
  val objectTypeBuilder: ObjectTypeBuilder = new ObjectTypeBuilder(project = project, nodeInterface = Some(nodeInterface))
  val objectTypes                          = objectTypeBuilder.modelObjectTypes
  val connectionTypes                      = objectTypeBuilder.modelConnectionTypes
  val outputTypesBuilder                   = OutputTypesBuilder(project, objectTypes, dataResolver)
  val pluralsCache                         = new PluralsCache
  val databaseMutactionExecutor            = apiDependencies.databaseMutactionExecutor
  val sideEffectMutactionExecutor          = apiDependencies.sideEffectMutactionExecutor
  val mutactionVerifier                    = apiDependencies.mutactionVerifier

  def build(): Schema[ApiUserContext, Unit] = ApiMetrics.schemaBuilderTimer.time(project.id) {
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
    val fields = project.models.map(getAllItemsField) ++
      project.models.flatMap(getSingleItemField) ++
      project.models.map(getAllItemsConnectionField) ++
      List(nodeField)

    ObjectType("Query", fields)
  }

  def buildMutation(): Option[ObjectType[ApiUserContext, Unit]] = {
    val fields = project.models.map(createItemField) ++
      project.models.flatMap(updateItemField) ++
      project.models.flatMap(deleteItemField) ++
      project.models.flatMap(upsertItemField) ++
      project.models.flatMap(updateManyField) ++
      project.models.map(deleteManyField)
    Some(ObjectType("Mutation", fields))
  }

  def buildSubscription(): Option[ObjectType[ApiUserContext, Unit]] = {
    val subscriptionFields = project.models.map(getSubscriptionField)

    if (subscriptionFields.isEmpty) None
    else Some(ObjectType("Subscription", subscriptionFields))
  }

  def getAllItemsField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      camelCase(pluralsCache.pluralName(model)),
      fieldType = ListType(OptionType(objectTypes(model.name))),
      arguments = objectTypeBuilder.mapToListConnectionArguments(model),
      resolve = (ctx) => {
        val arguments = objectTypeBuilder.extractQueryArgumentsFromContext(model, ctx)
        DeferredValue(ManyModelDeferred(model, arguments)).map(_.toNodes.map(Some(_)))
      }
    )
  }

  def getAllItemsConnectionField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"${camelCase(pluralsCache.pluralName(model))}Connection",
      fieldType = connectionTypes(model.name),
      arguments = objectTypeBuilder.mapToListConnectionArguments(model),
      resolve = (ctx) => {
        val arguments = objectTypeBuilder.extractQueryArgumentsFromContext(model, ctx)
        DeferredValue(ManyModelDeferred(model, arguments))
      }
    )
  }

  def getSingleItemField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder
      .whereUniqueArgument(model)
      .map { whereArg =>
        Field(
          camelCase(model.name),
          fieldType = OptionType(objectTypes(model.name)),
          arguments = List(whereArg),
          resolve = (ctx) => {
            val coolArgs = CoolArgs(ctx.args.raw)
            val where    = coolArgs.extractNodeSelectorFromWhereField(model)
            OneDeferred(model, where.field.name, where.unwrappedFieldValue)
          }
        )
      }
  }

  def createItemField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"create${model.name}",
      fieldType = outputTypesBuilder.mapCreateOutputType(model, objectTypes(model.name)),
      arguments = argumentsBuilder.getSangriaArgumentsForCreate(model).getOrElse(List.empty),
      resolve = (ctx) => {
        val mutation       = Create(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
        val mutationResult = ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier)
        mapReturnValueResult(mutationResult, ctx.args)
      }
    )
  }

  def updateItemField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder.getSangriaArgumentsForUpdate(model).map { args =>
      Field(
        s"update${model.name}",
        fieldType = OptionType(outputTypesBuilder.mapUpdateOutputType(model, objectTypes(model.name))),
        arguments = args,
        resolve = (ctx) => {
          val mutation = Update(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)

          val mutationResult = ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier)
          mapReturnValueResult(mutationResult, ctx.args)
        }
      )
    }
  }

  def updateManyField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder.getSangriaArgumentsForUpdateMany(model).map { args =>
      Field(
        s"updateMany${pluralsCache.pluralName(model)}",
        fieldType = objectTypeBuilder.batchPayloadType,
        arguments = args,
        resolve = (ctx) => {
          val where    = objectTypeBuilder.extractRequiredFilterFromContext(model, ctx)
          val mutation = UpdateMany(project, model, ctx.args, where, dataResolver = masterDataResolver)
          ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier)
        }
      )
    }
  }

  def upsertItemField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder.getSangriaArgumentsForUpsert(model).map { args =>
      Field(
        s"upsert${model.name}",
        fieldType = outputTypesBuilder.mapUpsertOutputType(model, objectTypes(model.name)),
        arguments = args,
        resolve = (ctx) => {
          val mutation       = Upsert(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
          val mutationResult = ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier)
          mapReturnValueResult(mutationResult, ctx.args)
        }
      )
    }
  }

  def deleteItemField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder.getSangriaArgumentsForDelete(model).map { args =>
      Field(
        s"delete${model.name}",
        fieldType = OptionType(outputTypesBuilder.mapDeleteOutputType(model, objectTypes(model.name), onlyId = false)),
        arguments = args,
        resolve = (ctx) => {
          val mutation = Delete(
            model = model,
            modelObjectTypes = objectTypeBuilder,
            project = project,
            args = ctx.args,
            dataResolver = masterDataResolver
          )
          val mutationResult = ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier)
          mapReturnValueResult(mutationResult, ctx.args)
        }
      )
    }
  }

  def deleteManyField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"deleteMany${pluralsCache.pluralName(model)}",
      fieldType = objectTypeBuilder.batchPayloadType,
      arguments = argumentsBuilder.getSangriaArgumentsForDeleteMany(model),
      resolve = (ctx) => {
        val where    = objectTypeBuilder.extractRequiredFilterFromContext(model, ctx)
        val mutation = DeleteMany(project, model, where, dataResolver = masterDataResolver)
        ClientMutationRunner.run(mutation, databaseMutactionExecutor, sideEffectMutactionExecutor, mutactionVerifier)
      }
    )
  }

  def getSubscriptionField(model: Model): Field[ApiUserContext, Unit] = {
    val objectType = objectTypes(model.name)

    Field(
      camelCase(model.name),
      fieldType = OptionType(outputTypesBuilder.mapSubscriptionOutputType(model, objectType)),
      arguments = List(SangriaQueryArguments.whereSubscriptionArgument(model = model, project = project)),
      resolve = _ => None
    )
  }

  implicit val nodeEvidence = SangriaEvidences.DataItemNodeEvidence

  lazy val NodeDefinition(nodeInterface: InterfaceType[ApiUserContext, DataItem], nodeField, nodeRes) = Node.definitionById(
    resolve = (id: String, ctx: Context[ApiUserContext, Unit]) => {
      dataResolver.resolveByGlobalId(id).map(x => x.map(_.toDataItem))
    },
    possibleTypes = {
      objectTypes.values.flatMap { o =>
        if (o.allInterfaces.exists(_.name == "Node")) {
          Some(PossibleNodeObject[ApiUserContext, Node, DataItem](o))
        } else {
          None
        }
      }.toList
    }
  )

  def camelCase(string: String): String = Character.toLowerCase(string.charAt(0)) + string.substring(1)

  private def mapReturnValueResult(result: Future[ReturnValueResult], args: Args): Future[SimpleResolveOutput] = {
    result.map {
      case ReturnValue(dataItem) => outputTypesBuilder.mapResolve(dataItem, args)
      case NoReturnValue(where)  => throw APIErrors.NodeNotFoundForWhereError(where)
    }
  }
}

object SangriaEvidences {
  implicit object DataItemNodeEvidence extends IdentifiableNode[ApiUserContext, DataItem] {
    override def id(ctx: Context[ApiUserContext, DataItem]) = ctx.value.id
  }
}

class PluralsCache {
  private val cache = mutable.Map.empty[Model, String]

  def pluralName(model: Model): String = cache.getOrElseUpdate(
    key = model,
    op = English.plural(model.name).capitalize
  )
}
