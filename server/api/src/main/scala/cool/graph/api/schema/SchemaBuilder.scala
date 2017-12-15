package cool.graph.api.schema

import akka.actor.ActorSystem
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataItem
import cool.graph.api.database.DeferredTypes.{ManyModelDeferred, OneDeferred}
import cool.graph.api.mutations.{ClientMutationRunner, CoolArgs}
import cool.graph.api.mutations.mutations._
import cool.graph.shared.models.{Model, Project}
import org.atteo.evo.inflector.English
import sangria.relay.{Node, NodeDefinition, PossibleNodeObject}
import sangria.schema._

import scala.collection.mutable

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

  val argumentsBuilder   = ArgumentsBuilder(project = project)
  val dataResolver       = apiDependencies.dataResolver(project)
  val masterDataResolver = apiDependencies.masterDataResolver(project)
  val objectTypeBuilder  = new ObjectTypeBuilder(project = project, nodeInterface = Some(nodeInterface))
  val objectTypes        = objectTypeBuilder.modelObjectTypes
  val conectionTypes     = objectTypeBuilder.modelConnectionTypes
  val outputTypesBuilder = OutputTypesBuilder(project, objectTypes, dataResolver)
  val pluralsCache       = new PluralsCache

  def build(): Schema[ApiUserContext, Unit] = {
    val query        = buildQuery()
    val mutation     = buildMutation()
    val subscription = buildSubscription()

    Schema(
      query = query,
      mutation = mutation,
      validationRules = SchemaValidationRule.empty
    )
  }

  def buildQuery(): ObjectType[ApiUserContext, Unit] = {

    val fields = project.models.map(getAllItemsField) ++
      project.models.flatMap(getSingleItemField) ++
      project.models.map(getAllItemsConnectionField) :+
      nodeField

    ObjectType("Query", fields)
  }

  def buildMutation(): Option[ObjectType[ApiUserContext, Unit]] = {
    val fields = project.models.map(createItemField) ++
      project.models.flatMap(updateItemField) ++
      project.models.flatMap(deleteItemField)

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
      fieldType = ListType(objectTypes(model.name)),
      arguments = objectTypeBuilder.mapToListConnectionArguments(model),
      resolve = (ctx) => {
        val arguments = objectTypeBuilder.extractQueryArgumentsFromContext(model, ctx)

        DeferredValue(ManyModelDeferred(model, arguments)).map(_.toNodes)
      }
    )
  }

  def getAllItemsConnectionField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"${camelCase(pluralsCache.pluralName(model))}Connection",
      fieldType = conectionTypes(model.name),
      arguments = objectTypeBuilder.mapToListConnectionArguments(model),
      resolve = (ctx) => {
        val arguments = objectTypeBuilder.extractQueryArgumentsFromContext(model, ctx)

        DeferredValue(ManyModelDeferred(model, arguments))
      }
    )
  }

  def getSingleItemField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder
      .whereArgument(model)
      .map { whereArg =>
        Field(
          camelCase(model.name),
          fieldType = OptionType(objectTypes(model.name)),
          arguments = List(whereArg),
          resolve = (ctx) => {
            val coolArgs = CoolArgs(ctx.args.raw)
            val where    = coolArgs.extractNodeSelectorFromWhereField(model)
            OneDeferred(model, where.fieldName, where.unwrappedFieldValue)
          }
        )
      }
  }

  def createItemField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"create${model.name}",
      fieldType = outputTypesBuilder.mapCreateOutputType(model, objectTypes(model.name)),
      arguments = argumentsBuilder.getSangriaArgumentsForCreate(model),
      resolve = (ctx) => {
        val mutation = Create(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
        ClientMutationRunner
          .run(mutation, dataResolver)
          .map(outputTypesBuilder.mapResolve(_, ctx.args))
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

          ClientMutationRunner
            .run(mutation, dataResolver)
            .map(outputTypesBuilder.mapResolve(_, ctx.args))
        }
      )
    }
  }

  def updateOrCreateItemField(model: Model): Option[Field[ApiUserContext, Unit]] = {
    argumentsBuilder.getSangriaArgumentsForUpdateOrCreate(model).map { args =>
      Field(
        s"updateOrCreate${model.name}",
        fieldType = OptionType(outputTypesBuilder.mapUpdateOrCreateOutputType(model, objectTypes(model.name))),
        arguments = args,
        resolve = (ctx) => {
          val mutation = UpdateOrCreate(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
          ClientMutationRunner
            .run(mutation, dataResolver)
            .map(outputTypesBuilder.mapResolve(_, ctx.args))
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
          ClientMutationRunner
            .run(mutation, dataResolver)
            .map(outputTypesBuilder.mapResolve(_, ctx.args))
        }
      )
    }
  }

  def getSubscriptionField(model: Model): Field[ApiUserContext, Unit] = {
    val objectType = objectTypes(model.name)

    Field(
      s"${model.name}",
      fieldType = OptionType(outputTypesBuilder.mapSubscriptionOutputType(model, objectType)),
      arguments = List(SangriaQueryArguments.whereSubscriptionArgument(model = model, project = project)),
      resolve = _ => None
    )
  }

  lazy val NodeDefinition(nodeInterface: InterfaceType[ApiUserContext, DataItem], nodeField, nodeRes) = Node.definitionById(
    resolve = (id: String, ctx: Context[ApiUserContext, Unit]) => {
      dataResolver.resolveByGlobalId(id)
    },
    possibleTypes = {
      objectTypes.values.flatMap { o =>
        if (o.allInterfaces.exists(_.name == "Node")) {
          Some(PossibleNodeObject(o))
        } else {
          None
        }
      }.toList
    }
  )

  def camelCase(string: String): String = Character.toLowerCase(string.charAt(0)) + string.substring(1)
}

class PluralsCache {
  private val cache = mutable.Map.empty[Model, String]

  def pluralName(model: Model): String = cache.getOrElseUpdate(
    key = model,
    op = English.plural(model.name).capitalize
  )
}
