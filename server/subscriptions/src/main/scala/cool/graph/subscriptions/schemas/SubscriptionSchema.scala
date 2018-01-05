package cool.graph.subscriptions.schemas

import cool.graph.api.database.DataItem
import cool.graph.api.schema._
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.{Model, ModelMutationType, Project}
import cool.graph.subscriptions.SubscriptionDependencies
import cool.graph.subscriptions.resolving.SubscriptionUserContext
import sangria.schema._

import scala.concurrent.Future

case class SubscriptionSchema(
    model: Model,
    project: Project,
    updatedFields: Option[List[String]],
    mutation: ModelMutationType,
    previousValues: Option[DataItem],
    externalSchema: Boolean = false
)(implicit dependencies: SubscriptionDependencies) {
  val isDelete: Boolean = mutation == ModelMutationType.Deleted

  import dependencies.system

  val schemaBuilder                                                       = SchemaBuilderImpl(project)
  val modelObjectTypes: Map[String, ObjectType[ApiUserContext, DataItem]] = schemaBuilder.objectTypes
  val outputMapper                                                        = OutputTypesBuilder(project, modelObjectTypes, dependencies.dataResolver(project))

  val subscriptionField: Field[SubscriptionUserContext, Unit] = Field(
    camelCase(model.name),
    description = Some("The updated node"),
    fieldType = OptionType(
      outputMapper
        .mapSubscriptionOutputType(
          model,
          modelObjectTypes(model.name),
          updatedFields,
          mutation,
          previousValues,
          isDelete match {
            case false => None
            case true  => Some(SimpleResolveOutput(DataItem("", Map.empty), Args.empty))
          }
        )),
    arguments = List(
      externalSchema match {
        case false => SangriaQueryArguments.internalWhereSubscriptionArgument(model = model, project = project)
        case true  => SangriaQueryArguments.whereSubscriptionArgument(model = model, project = project)
      }
    ),
    resolve = (ctx) =>
      isDelete match {
        case false =>
          SubscriptionDataResolver.resolve(dependencies.dataResolver(project), schemaBuilder.objectTypeBuilder, model, ctx)

        case true =>
//        Future.successful(None)
          // in the delete case there MUST be the previousValues
          Future.successful(Some(SimpleResolveOutput(previousValues.get, Args.empty)))
    }
  )

  val createDummyField: Field[SubscriptionUserContext, Unit] = Field(
    "dummy",
    description = Some("This is only a dummy field due to the API of Schema of Sangria, as Query is not optional"),
    fieldType = StringType,
    resolve = (ctx) => ""
  )

  def build(): Schema[SubscriptionUserContext, Unit] = {
    val Subscription = Some(
      ObjectType(
        "Subscription",
        List(subscriptionField)
      ))

    val Query = ObjectType(
      "Query",
      List(createDummyField)
    )

    Schema(Query, None, Subscription)
  }

  def camelCase(string: String): String = Character.toLowerCase(string.charAt(0)) + string.substring(1)
}
