package com.prisma.subscriptions.schema

import com.prisma.api.ApiDependencies
import com.prisma.api.connector.PrismaNode
import com.prisma.api.schema._
import com.prisma.shared.models.ModelMutationType.ModelMutationType
import com.prisma.shared.models.{Model, ModelMutationType, Project}
import com.prisma.subscriptions.SubscriptionUserContext
import sangria.schema._

import scala.concurrent.Future

case class SubscriptionSchema(
    model: Model,
    project: Project,
    updatedFields: Option[List[String]],
    mutation: ModelMutationType,
    previousValues: Option[PrismaNode],
    externalSchema: Boolean = false
)(implicit dependencies: ApiDependencies) {
  val isDelete: Boolean = mutation == ModelMutationType.Deleted

  import dependencies.system

  val schemaBuilder    = SchemaBuilderImpl(project, dependencies.capabilities, enableRawAccess = false)
  val modelObjectTypes = schemaBuilder.objectTypes
  val outputMapper     = OutputTypesBuilder(project, modelObjectTypes, dependencies.dataResolver(project))

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
            case true  => Some(PrismaNode.dummy)
          }
        )),
    arguments = List(
      externalSchema match {
        case false =>
          SangriaQueryArguments.internalWhereSubscriptionArgument(model = model, project = project, capabilities = dependencies.apiConnector.capabilities)
        case true => SangriaQueryArguments.whereSubscriptionArgument(model = model, project = project, capabilities = dependencies.apiConnector.capabilities)
      }
    ),
    resolve = (ctx) =>
      isDelete match { // in the delete case there MUST be the previousValues
        case true  => Future.successful(previousValues)
        case false => SubscriptionDataResolver.resolve(dependencies.dataResolver(project), schemaBuilder.objectTypeBuilder, model, ctx)
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
