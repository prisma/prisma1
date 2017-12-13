package cool.graph.api.schema

import akka.actor.ActorSystem
import cool.graph.api.ApiDependencies
import cool.graph.api.database.DataItem
import cool.graph.api.database.DeferredTypes.{ManyModelDeferred, OneDeferred}
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

  val dataResolver       = apiDependencies.dataResolver(project)
  val masterDataResolver = apiDependencies.masterDataResolver(project)
  val objectTypeBuilder  = new ObjectTypeBuilder(project = project, nodeInterface = Some(nodeInterface))
  val objectTypes        = objectTypeBuilder.modelObjectTypes
  val conectionTypes     = objectTypeBuilder.modelConnectionTypes
  val inputTypesBuilder  = InputTypesBuilder(project = project)
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
      project.models.map(getSingleItemField) ++
      project.models.map(getAllItemsConnectionField) :+
      nodeField

    ObjectType("Query", fields)
  }

  def buildMutation(): Option[ObjectType[ApiUserContext, Unit]] = {

    val fields = project.models.map(createItemField) ++
      project.models.map(updateItemField) ++
      project.models.map(deleteItemField)

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

  def getSingleItemField(model: Model): Field[ApiUserContext, Unit] = {
    val arguments = objectTypeBuilder.mapToUniqueArguments(model)

    Field(
      camelCase(model.name),
      fieldType = OptionType(objectTypes(model.name)),
      arguments = arguments,
      resolve = (ctx) => {

        val arg = arguments.find(a => ctx.args.argOpt(a.name).isDefined) match {
          case Some(value) => value
          case None =>
            ??? //throw UserAPIErrors.GraphQLArgumentsException(s"None of the following arguments provided: ${arguments.map(_.name)}")
        }

//        dataResolver
//          .batchResolveByUnique(model, arg.name, List(ctx.arg(arg).asInstanceOf[Option[_]].get))
//          .map(_.headOption)
        // todo: Make OneDeferredResolver.dataItemsToToOneDeferredResultType work with Timestamps
        OneDeferred(model, arg.name, ctx.arg(arg).asInstanceOf[Option[_]].get)
      }
    )
  }

  def createItemField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"create${model.name}",
      fieldType = outputTypesBuilder.mapCreateOutputType(model, objectTypes(model.name)),
      arguments = inputTypesBuilder.getSangriaArgumentsForCreate(model),
      resolve = (ctx) => {
        val mutation = new Create(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
        mutation
          .run(ctx.ctx)
          .map(outputTypesBuilder.mapResolve(_, ctx.args))
      }
    )
  }

  def updateItemField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"update${model.name}",
      fieldType = OptionType(outputTypesBuilder.mapUpdateOutputType(model, objectTypes(model.name))),
      arguments = inputTypesBuilder.getSangriaArgumentsForUpdate(model),
      resolve = (ctx) => {
        new Update(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
          .run(ctx.ctx)
          .map(outputTypesBuilder.mapResolve(_, ctx.args))
      }
    )
  }

  def updateOrCreateItemField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"updateOrCreate${model.name}",
      fieldType = OptionType(outputTypesBuilder.mapUpdateOrCreateOutputType(model, objectTypes(model.name))),
      arguments = inputTypesBuilder.getSangriaArgumentsForUpdateOrCreate(model),
      resolve = (ctx) => {
        new UpdateOrCreate(model = model, project = project, args = ctx.args, dataResolver = masterDataResolver)
          .run(ctx.ctx)
          .map(outputTypesBuilder.mapResolve(_, ctx.args))
      }
    )
  }

  def deleteItemField(model: Model): Field[ApiUserContext, Unit] = {
    Field(
      s"delete${model.name}",
      fieldType = OptionType(outputTypesBuilder.mapDeleteOutputType(model, objectTypes(model.name), onlyId = false)),
      arguments = inputTypesBuilder.getSangriaArgumentsForDelete(model),
      resolve = (ctx) => {
        new Delete(
          model = model,
          modelObjectTypes = objectTypeBuilder,
          project = project,
          args = ctx.args,
          dataResolver = masterDataResolver
        ).run(ctx.ctx)
          .map(outputTypesBuilder.mapResolve(_, ctx.args))
      }
    )
  }

  def getSubscriptionField(model: Model): Field[ApiUserContext, Unit] = {

    val objectType = objectTypes(model.name)
    Field(
      s"${model.name}",
      fieldType = OptionType(outputTypesBuilder.mapSubscriptionOutputType(model, objectType)),
      arguments = List(SangriaQueryArguments.filterSubscriptionArgument(model = model, project = project)),
      resolve = _ => None
    )

  }

  lazy val NodeDefinition(nodeInterface: InterfaceType[ApiUserContext, DataItem], nodeField, nodeRes) = Node.definitionById(
    resolve = (id: String, ctx: Context[ApiUserContext, Unit]) => {
      dataResolver.resolveByGlobalId(id)
    },
    possibleTypes = {
      objectTypes.values.map(o => PossibleNodeObject(o)).toList
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
