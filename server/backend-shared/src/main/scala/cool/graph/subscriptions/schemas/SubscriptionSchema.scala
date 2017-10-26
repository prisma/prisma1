package cool.graph.subscriptions.schemas

import cool.graph.DataItem
import cool.graph.client.schema.simple.{SimpleOutputMapper, SimpleResolveOutput, SimpleSchemaModelObjectTypeBuilder}
import cool.graph.client.{SangriaQueryArguments, UserContext}
import cool.graph.shared.models.ModelMutationType.ModelMutationType
import cool.graph.shared.models.{Model, ModelMutationType, Project}
import cool.graph.subscriptions.SubscriptionUserContext
import sangria.schema._
import scaldi.{Injectable, Injector}

import scala.concurrent.Future

case class SubscriptionSchema[ManyDataItemType](model: Model,
                                                project: Project,
                                                updatedFields: Option[List[String]],
                                                mutation: ModelMutationType,
                                                previousValues: Option[DataItem],
                                                externalSchema: Boolean = false)(implicit inj: Injector)
    extends Injectable {
  val isDelete: Boolean = mutation == ModelMutationType.Deleted

  val schemaBuilder                                                    = new SimpleSchemaModelObjectTypeBuilder(project)
  val modelObjectTypes: Map[String, ObjectType[UserContext, DataItem]] = schemaBuilder.modelObjectTypes
  val outputMapper                                                     = SimpleOutputMapper(project, modelObjectTypes)

  val subscriptionField: Field[SubscriptionUserContext, Unit] = Field(
    s"${model.name}",
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
        case false => SangriaQueryArguments.internalFilterSubscriptionArgument(model = model, project = project)
        case true  => SangriaQueryArguments.filterSubscriptionArgument(model = model, project = project)
      }
    ),
    resolve = (ctx) =>
      isDelete match {
        case false =>
          SubscriptionDataResolver.resolve(schemaBuilder, model, ctx)

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
}
